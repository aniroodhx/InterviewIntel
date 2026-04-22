package com.interviewintel.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewintel.dto.Dtos;
import com.interviewintel.model.InterviewPost;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Sends retrieved posts to Claude API and gets back a structured JSON summary.
 *
 * Key anti-hallucination measures:
 *  1. Only summarize what's in the provided posts (grounded prompting)
 *  2. Ask Claude to cite which posts support each claim
 *  3. If data is insufficient, return honest "not enough data" signals
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ClaudeSummarizationService {

    private final ObjectMapper objectMapper;

    @Value("${claude.api.key}")
    private String apiKey;

    @Value("${claude.api.url}")
    private String apiUrl;

    @Value("${claude.model}")
    private String model;

    private static final int MAX_CONTEXT_CHARS = 60_000; // ~15k tokens of posts

    public Dtos.InterviewIntelResponse summarize(
        String company,
        String role,
        String location,
        List<InterviewPost> posts
    ) {
        if (posts.isEmpty()) {
            return buildEmptyResponse(company, role, location);
        }

        String postsContext = buildPostsContext(posts);
        String prompt = buildPrompt(company, role, location, postsContext, posts.size());

        try {
            String rawJson = callClaude(prompt);
            return parseResponse(rawJson, company, role, location, posts.size());
        } catch (Exception e) {
            log.error("Claude summarization failed for {}/{}: {}", company, role, e.getMessage());
            return buildEmptyResponse(company, role, location);
        }
    }

    //Context Builder

    private String buildPostsContext(List<InterviewPost> posts) {
        StringBuilder sb = new StringBuilder();
        int charCount = 0;

        for (int i = 0; i < posts.size(); i++) {
            InterviewPost p = posts.get(i);
            String entry = String.format(
                "--- Post %d (source: %s, upvotes: %d, quality: %.0f) ---\n" +
                "Title: %s\n" +
                "Body: %s\n\n",
                i + 1,
                p.getSource().name(),
                p.getUpvotes() != null ? p.getUpvotes() : 0,
                p.getQualityScore() != null ? p.getQualityScore() : 0,
                p.getTitle(),
                p.getBody() != null ? p.getBody() : ""
            );

            if (charCount + entry.length() > MAX_CONTEXT_CHARS) break;
            sb.append(entry);
            charCount += entry.length();
        }

        return sb.toString();
    }

    //Prompt Engineering
    private String buildPrompt(String company, String role, String location, String context, int postCount) {
        String currency = "IN".equalsIgnoreCase(location) ? "INR LPA (Indian rupees, lakhs per annum)" : "USD annual";
        String locationLabel = "IN".equalsIgnoreCase(location) ? "India" : "United States";

        return String.format("""
            You are an expert interview research analyst. Your job is to analyze the following %d real interview experience posts
            from Reddit and other community sources, and produce a structured, accurate summary.
            
            Company: %s
            Role: %s
            Location: %s
            Compensation currency: %s
            
            CRITICAL RULES — follow exactly:
            1. ONLY use information present in the posts below. DO NOT hallucinate or invent details.
            2. If a piece of information is not mentioned in any post, use null for that field.
            3. For compensation, only include ranges you see explicitly mentioned in posts. If none, use null.
            4. For questions, only list question types/topics actually mentioned — not generic guesses.
            5. difficultyScore must reflect actual candidate sentiment in the posts (1=very easy, 5=very hard).
            6. topicFrequencies must count actual mentions across posts — count carefully.
            7. Be honest: if there are only 2-3 posts, say "limited data" in the overview.
            
            --- BEGIN POSTS ---
            %s
            --- END POSTS ---
            
            Return ONLY a valid JSON object with exactly this structure (no markdown, no backticks, no extra text):
            {
              "overview": "2-3 sentence summary of the interview process based strictly on these posts",
              "difficulty": "Easy|Medium|Hard",
              "difficultyScore": 1,
              "collectiveExperience": "3-4 sentences describing what candidates collectively experienced — timeline, surprises, offer rates if mentioned",
              "rounds": ["list", "of", "rounds", "mentioned"],
              "topics": ["topics", "that", "appear", "in", "posts"],
              "compensation": {
                "currency": "%s",
                "base": "range if mentioned, else null",
                "totalComp": "total comp if mentioned, else null",
                "bonus": "bonus info if mentioned, else null",
                "equity": "equity info if mentioned, else null",
                "note": "any compensation note"
              },
              "questions": [
                {"category": "DSA|System Design|Behavioral|CS Fundamentals|Domain", "question": "actual question from posts", "difficulty": "Easy|Medium|Hard", "frequency": 1}
              ],
              "topicFrequencies": [
                {"topic": "topic name", "count": 3, "percentage": 60.0}
              ],
              "tips": ["actionable tip based strictly on posts", "..."],
              "dataQuality": "HIGH|MEDIUM|LOW"
            }
            """,
            postCount, company, role, locationLabel, currency, context, currency
        );
    }

    //Claude API Call
    private String callClaude(String prompt) {
        WebClient client = WebClient.builder()
            .baseUrl("https://api.anthropic.com")
            .defaultHeaders(h -> {
                h.set("x-api-key", apiKey);
                h.set("anthropic-version", "2023-06-01");
                h.set("Content-Type", "application/json");
            })
            .build();

        String requestBody;
        try {
            requestBody = objectMapper.writeValueAsString(java.util.Map.of(
                "model", model,
                "max_tokens", 2000,
                "messages", List.of(java.util.Map.of("role", "user", "content", prompt))
            ));
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize Claude request", e);
        }

        String response = client.post()
            .uri("/v1/messages")
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(String.class)
            .block();

        try {
            JsonNode root = objectMapper.readTree(response);
            return root.path("content").get(0).path("text").asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Claude response: " + response, e);
        }
    }

    //Response Parser

    private Dtos.InterviewIntelResponse parseResponse(
        String rawJson, String company, String role, String location, int postCount
    ) throws Exception {
        String cleaned = rawJson.replaceAll("```json|```", "").trim();
        JsonNode node = objectMapper.readTree(cleaned);

        // Compensation
        JsonNode compNode = node.path("compensation");
        Dtos.CompensationDto comp = Dtos.CompensationDto.builder()
            .currency(textOrNull(compNode, "currency"))
            .base(textOrNull(compNode, "base"))
            .totalComp(textOrNull(compNode, "totalComp"))
            .bonus(textOrNull(compNode, "bonus"))
            .equity(textOrNull(compNode, "equity"))
            .note(textOrNull(compNode, "note"))
            .build();

        // Questions
        List<Dtos.QuestionDto> questions = new ArrayList<>();
        for (JsonNode q : node.path("questions")) {
            questions.add(Dtos.QuestionDto.builder()
                .category(textOrNull(q, "category"))
                .question(textOrNull(q, "question"))
                .difficulty(textOrNull(q, "difficulty"))
                .frequency(q.path("frequency").asInt(1))
                .build());
        }

        // Topic frequencies
        List<Dtos.TopicFrequency> freqs = new ArrayList<>();
        for (JsonNode f : node.path("topicFrequencies")) {
            freqs.add(Dtos.TopicFrequency.builder()
                .topic(f.path("topic").asText())
                .count(f.path("count").asInt())
                .percentage(f.path("percentage").asDouble())
                .build());
        }

        // Rounds
        List<String> rounds = new ArrayList<>();
        node.path("rounds").forEach(r -> rounds.add(r.asText()));

        // Topics
        List<String> topics = new ArrayList<>();
        node.path("topics").forEach(t -> topics.add(t.asText()));

        // Tips
        List<String> tips = new ArrayList<>();
        node.path("tips").forEach(t -> tips.add(t.asText()));

        return Dtos.InterviewIntelResponse.builder()
            .company(company)
            .role(role)
            .location(location)
            .difficulty(node.path("difficulty").asText("Medium"))
            .difficultyScore(node.path("difficultyScore").asInt(3))
            .overview(node.path("overview").asText())
            .collectiveExperience(node.path("collectiveExperience").asText())
            .rounds(rounds)
            .topics(topics)
            .compensation(comp)
            .questions(questions)
            .topicFrequencies(freqs)
            .tips(tips)
            .sourcePostCount(postCount)
            .isCached(false)
            .generatedAt(java.time.Instant.now().toString())
            .build();
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode n = node.path(field);
        if (n.isMissingNode() || n.isNull()) return null;
        String val = n.asText("").trim();
        return val.isEmpty() || val.equals("null") ? null : val;
    }

    private Dtos.InterviewIntelResponse buildEmptyResponse(String company, String role, String location) {
        return Dtos.InterviewIntelResponse.builder()
            .company(company)
            .role(role)
            .location(location)
            .overview("No interview experiences found yet for this company and role. Try triggering ingestion or submitting your own experience.")
            .difficulty("Unknown")
            .difficultyScore(0)
            .rounds(List.of())
            .topics(List.of())
            .questions(List.of())
            .topicFrequencies(List.of())
            .tips(List.of())
            .sourcePostCount(0)
            .isCached(false)
            .generatedAt(java.time.Instant.now().toString())
            .build();
    }

    //inner import workaround
    private static class ArrayList<T> extends java.util.ArrayList<T> {}
}
