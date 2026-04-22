package com.interviewintel.ingestion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewintel.model.InterviewPost;
import com.interviewintel.repository.InterviewPostRepository;
import com.interviewintel.service.QueryNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fetches interview-related posts from Reddit using the official OAuth API.
 *
 * Reddit subreddits targeted:
 *   r/cscareerquestions, r/cscareerquestionsindia, r/india, r/leetcode, r/interviews
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedditIngestionService {

    private final InterviewPostRepository postRepository;
    private final QueryNormalizer normalizer;
    private final ObjectMapper objectMapper;

    @Value("${reddit.client.id}")
    private String clientId;

    @Value("${reddit.client.secret}")
    private String clientSecret;

    @Value("${reddit.user.agent}")
    private String userAgent;

    private static final List<String> SUBREDDITS = List.of(
        "cscareerquestions", "cscareerquestionsindia", "india",
        "leetcode", "interviews", "experienceddevs", "developersIndia"
    );

    private static final List<String> INTERVIEW_KEYWORDS = List.of(
        "interview experience", "interview report", "got offer", "rejected",
        "onsite", "virtual onsite", "final round", "coding interview",
        "system design interview", "behavioral interview", "got the offer",
        "interview process", "interview feedback"
    );

    /**
     * Main ingestion entry point — call this for a specific company+role.
     */
    @Async
    public void ingestForCompanyRole(String company, String role, String location) {
        log.info("Starting Reddit ingestion: company={}, role={}, location={}", company, role, location);

        String token = fetchOAuthToken();
        if (token == null) {
            log.error("Could not get Reddit OAuth token — check credentials");
            return;
        }

        AtomicInteger ingested = new AtomicInteger(0);
        String normalizedCompany = normalizer.normalizeCompany(company);
        String normalizedRole = normalizer.normalizeRole(role);

        for (String subreddit : SUBREDDITS) {
            List<JsonNode> posts = searchSubreddit(token, subreddit, company, role);
            for (JsonNode post : posts) {
                try {
                    String extId = post.path("id").asText();
                    InterviewPost.Source src = InterviewPost.Source.REDDIT;

                    if (postRepository.existsByExternalIdAndSource(extId, src)) continue;

                    String title = post.path("title").asText("");
                    String body  = post.path("selftext").asText("");

                    // Filter: must look like an actual interview experience
                    if (!isInterviewExperience(title, body)) continue;

                    double quality = computeQuality(post);

                    InterviewPost entity = InterviewPost.builder()
                        .externalId(extId)
                        .source(src)
                        .normalizedCompany(normalizedCompany)
                        .normalizedRole(normalizedRole)
                        .companyRaw(company)
                        .roleRaw(role)
                        .title(title)
                        .body(body.length() > 8000 ? body.substring(0, 8000) : body)
                        .url("https://reddit.com" + post.path("permalink").asText(""))
                        .author(post.path("author").asText(""))
                        .upvotes(post.path("score").asInt(0))
                        .commentCount(post.path("num_comments").asInt(0))
                        .subreddit(subreddit)
                        .createdAt(Instant.ofEpochSecond(post.path("created_utc").asLong()))
                        .ingestedAt(Instant.now())
                        .qualityScore(quality)
                        .isVerified(false)
                        .location(detectLocation(body + " " + title))
                        .build();

                    postRepository.save(entity);
                    ingested.incrementAndGet();
                } catch (Exception e) {
                    log.warn("Skipping post due to error: {}", e.getMessage());
                }
            }
        }
        log.info("Reddit ingestion complete: {} new posts saved", ingested.get());
    }

    //Reddit OAuth
    private String fetchOAuthToken() {
        try {
            WebClient client = WebClient.builder()
                .baseUrl("https://www.reddit.com")
                .defaultHeaders(h -> {
                    h.setBasicAuth(clientId, clientSecret);
                    h.set("User-Agent", userAgent);
                })
                .build();

            String response = client.post()
                .uri("/api/v1/access_token")
                .bodyValue("grant_type=client_credentials")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .retrieve()
                .bodyToMono(String.class)
                .block();

            JsonNode node = objectMapper.readTree(response);
            return node.path("access_token").asText(null);
        } catch (Exception e) {
            log.error("Reddit OAuth failed: {}", e.getMessage());
            return null;
        }
    }

    private List<JsonNode> searchSubreddit(String token, String subreddit, String company, String role) {
        List<JsonNode> results = new ArrayList<>();
        try {
            String query = String.format("%s %s interview experience", company, role);
            WebClient client = WebClient.builder()
                .baseUrl("https://oauth.reddit.com")
                .defaultHeaders(h -> {
                    h.setBearerAuth(token);
                    h.set("User-Agent", userAgent);
                })
                .build();

            String response = client.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/r/{subreddit}/search")
                    .queryParam("q", query)
                    .queryParam("restrict_sr", "true")
                    .queryParam("sort", "relevance")
                    .queryParam("limit", "25")
                    .queryParam("t", "all")
                    .build(subreddit))
                .retrieve()
                .bodyToMono(String.class)
                .block();

            JsonNode root = objectMapper.readTree(response);
            JsonNode children = root.path("data").path("children");
            for (JsonNode child : children) {
                results.add(child.path("data"));
            }
        } catch (Exception e) {
            log.warn("Search failed for r/{}: {}", subreddit, e.getMessage());
        }
        return results;
    }

    //Quality and Filtering
    private boolean isInterviewExperience(String title, String body) {
        String combined = (title + " " + body).toLowerCase();
        long matches = INTERVIEW_KEYWORDS.stream().filter(combined::contains).count();
        // Must match at least 1 keyword AND have meaningful body length
        return matches >= 1 && body.length() > 200;
    }

    private double computeQuality(JsonNode post) {
        double score = 0;
        int upvotes  = post.path("score").asInt(0);
        int comments = post.path("num_comments").asInt(0);
        int bodyLen  = post.path("selftext").asText("").length();

        // Upvotes: 0-40 points
        score += Math.min(40, upvotes * 0.5);
        // Comment engagement: 0-20 points
        score += Math.min(20, comments * 2);
        // Body length (more detail = better): 0-30 points
        score += Math.min(30, bodyLen / 100.0);
        // Awards/distinguished
        if (post.path("total_awards_received").asInt(0) > 0) score += 10;

        return Math.min(100, score);
    }

    private String detectLocation(String text) {
        String lower = text.toLowerCase();
        if (lower.contains("india") || lower.contains("bangalore") || lower.contains("hyderabad")
            || lower.contains("chennai") || lower.contains("pune") || lower.contains("noida")
            || lower.contains("lpa") || lower.contains("inr")) {
            return "IN";
        }
        if (lower.contains("usa") || lower.contains("seattle") || lower.contains("san francisco")
            || lower.contains("new york") || lower.contains("austin") || lower.contains("usd")) {
            return "US";
        }
        return null;
    }
}
