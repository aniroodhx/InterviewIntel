package com.interviewintel.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "search_results", indexes = {
    @Index(name = "idx_cache_key", columnList = "cacheKey", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String cacheKey;            // "american-express::software-engineer-1::IN"

    @Column(nullable = false)
    private String normalizedCompany;

    @Column(nullable = false)
    private String normalizedRole;

    private String location;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String resultJson;          // full structured JSON from LLM

    private Integer sourcePostCount;    // how many posts fed into this

    private Instant generatedAt;
    private Instant expiresAt;          // TTL: regenerate after 7 days

    private Long totalSearches;         // track popularity
}
