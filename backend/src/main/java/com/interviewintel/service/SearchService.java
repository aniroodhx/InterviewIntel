package com.interviewintel.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewintel.dto.Dtos;
import com.interviewintel.ingestion.RedditIngestionService;
import com.interviewintel.model.InterviewPost;
import com.interviewintel.model.SearchResult;
import com.interviewintel.repository.InterviewPostRepository;
import com.interviewintel.repository.SearchResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Orchestrates the full search flow:
 *
 *  1. Normalize query
 *  2. Check Redis cache (hot path — <1ms)
 *  3. Check PostgreSQL cache (warm path — <50ms)
 *  4. Retrieve posts from DB
 *  5. If too few posts, trigger async Reddit ingestion
 *  6. Call Claude for summarization
 *  7. Store result in both caches
 *  8. Return structured response
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {

    private static final int MIN_POSTS_FOR_SUMMARY = 3;
    private static final int MAX_POSTS_TO_SUMMARIZE = 20;
    private static final double MIN_QUALITY_THRESHOLD = 15.0;
    private static final Duration REDIS_TTL = Duration.ofHours(6);
    private static final Duration DB_CACHE_TTL = Duration.ofDays(7);
    private static final String REDIS_KEY_PREFIX = "intel:";

    private final QueryNormalizer normalizer;
    private final InterviewPostRepository postRepository;
    private final SearchResultRepository searchResultRepository;
    private final ClaudeSummarizationService claudeService;
    private final RedditIngestionService redditIngestionService;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    public Dtos.InterviewIntelResponse search(Dtos.SearchRequest request) {
        String normCompany = normalizer.normalizeCompany(request.getCompany());
        String normRole    = normalizer.normalizeRole(request.getRole());
        String location    = request.getLocation();
        String cacheKey    = normalizer.buildCacheKey(request.getCompany(), request.getRole(), location);
        String redisKey    = REDIS_KEY_PREFIX + cacheKey;

        log.info("Search: company='{}' → '{}', role='{}' → '{}', location={}",
            request.getCompany(), normCompany, request.getRole(), normRole, location);

        //Step 1: Redis hot cache
        String cached = redisTemplate.opsForValue().get(redisKey);
        if (cached != null) {
            log.info("Cache HIT (Redis): {}", cacheKey);
            return deserialize(cached, normCompany, normRole, location, true);
        }

        //Step 2: PostgreSQL warm cache
        Optional<SearchResult> dbCached = searchResultRepository.findByCacheKey(cacheKey);
        if (dbCached.isPresent() && dbCached.get().getExpiresAt().isAfter(Instant.now())) {
            log.info("Cache HIT (DB): {}", cacheKey);
            searchResultRepository.incrementSearchCount(cacheKey);
            // Repopulate Redis
            redisTemplate.opsForValue().set(redisKey, dbCached.get().getResultJson(), REDIS_TTL);
            return deserialize(dbCached.get().getResultJson(), normCompany, normRole, location, true);
        }

        //Step 3: Retrieve posts from DB
        List<InterviewPost> posts = retrievePosts(normCompany, normRole, location);
        log.info("Retrieved {} posts for {}/{}", posts.size(), normCompany, normRole);

        //Step 4: Trigger background ingestion if data is sparse
        if (posts.size() < MIN_POSTS_FOR_SUMMARY) {
            log.info("Sparse data ({} posts) — triggering async Reddit ingestion", posts.size());
            redditIngestionService.ingestForCompanyRole(request.getCompany(), request.getRole(), location);
        }

        //Step 5: Claude summarization
        List<InterviewPost> toSummarize = posts.size() > MAX_POSTS_TO_SUMMARIZE
            ? posts.subList(0, MAX_POSTS_TO_SUMMARIZE)
            : posts;

        Dtos.InterviewIntelResponse response = claudeService.summarize(
            normalizer.toDisplayName(normCompany),
            request.getRole(),
            location,
            toSummarize
        );

        //Attach source refs
        response.setSources(buildSourceRefs(toSummarize));

        //Step 6: Store in both caches
        try {
            String serialized = objectMapper.writeValueAsString(response);

            // Redis
            redisTemplate.opsForValue().set(redisKey, serialized, REDIS_TTL);

            // PostgreSQL
            SearchResult sr = SearchResult.builder()
                .cacheKey(cacheKey)
                .normalizedCompany(normCompany)
                .normalizedRole(normRole)
                .location(location)
                .resultJson(serialized)
                .sourcePostCount(posts.size())
                .generatedAt(Instant.now())
                .expiresAt(Instant.now().plus(DB_CACHE_TTL))
                .totalSearches(1L)
                .build();

            if (searchResultRepository.existsByCacheKey(cacheKey)) {
                searchResultRepository.findByCacheKey(cacheKey).ifPresent(existing -> {
                    existing.setResultJson(serialized);
                    existing.setSourcePostCount(posts.size());
                    existing.setGeneratedAt(Instant.now());
                    existing.setExpiresAt(Instant.now().plus(DB_CACHE_TTL));
                    searchResultRepository.save(existing);
                });
            } else {
                searchResultRepository.save(sr);
            }
        } catch (Exception e) {
            log.warn("Failed to cache result: {}", e.getMessage());
        }

        return response;
    }

    //Retrieval Strategy

    private List<InterviewPost> retrievePosts(String company, String role, String location) {
        // Primary: exact match
        List<InterviewPost> posts = postRepository.findByCompanyAndRole(
            company, role, location, MIN_QUALITY_THRESHOLD,
            PageRequest.of(0, MAX_POSTS_TO_SUMMARIZE)
        );

        if (posts.size() >= MIN_POSTS_FOR_SUMMARY) return posts;

        // Fallback 1: exact company, partial role (e.g., "engineer" matches "software engineer 1")
        String roleKeyword = extractRoleKeyword(role);
        List<InterviewPost> fuzzy = postRepository.findByCompanyFuzzy(
            company, roleKeyword, location,
            PageRequest.of(0, MAX_POSTS_TO_SUMMARIZE)
        );

        if (fuzzy.size() > posts.size()) posts = fuzzy;
        if (posts.size() >= MIN_POSTS_FOR_SUMMARY) return posts;

        // Fallback 2: location-agnostic search
        if (location != null) {
            List<InterviewPost> noLoc = postRepository.findByCompanyAndRole(
                company, role, null, 0,
                PageRequest.of(0, MAX_POSTS_TO_SUMMARIZE)
            );
            if (noLoc.size() > posts.size()) return noLoc;
        }

        return posts;
    }

    private String extractRoleKeyword(String normalizedRole) {
        // "software engineer 1" → "engineer"
        if (normalizedRole.contains("engineer")) return "engineer";
        if (normalizedRole.contains("scientist")) return "scientist";
        if (normalizedRole.contains("analyst")) return "analyst";
        if (normalizedRole.contains("manager")) return "manager";
        if (normalizedRole.contains("designer")) return "designer";
        return normalizedRole.split(" ")[0]; // first word
    }

    private List<Dtos.SourceRef> buildSourceRefs(List<InterviewPost> posts) {
        return posts.stream()
            .filter(p -> p.getUrl() != null)
            .limit(5)
            .map(p -> Dtos.SourceRef.builder()
                .platform(p.getSource().name())
                .url(p.getUrl())
                .title(p.getTitle() != null && p.getTitle().length() > 80
                    ? p.getTitle().substring(0, 80) + "..." : p.getTitle())
                .upvotes(p.getUpvotes())
                .build())
            .toList();
    }

    private Dtos.InterviewIntelResponse deserialize(String json, String company, String role, String location, boolean cached) {
        try {
            Dtos.InterviewIntelResponse r = objectMapper.readValue(json, Dtos.InterviewIntelResponse.class);
            r.setIsCached(cached);
            return r;
        } catch (Exception e) {
            log.error("Deserialization failed: {}", e.getMessage());
            return claudeService.summarize(company, role, location, List.of());
        }
    }

    //Autocomplete

    public List<String> autocompleteCompanies() {
        return postRepository.findDistinctCompanies();
    }

    public List<String> autocompleteRoles(String company) {
        return postRepository.findDistinctRolesForCompany(normalizer.normalizeCompany(company));
    }
}
