package com.interviewintel.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "interview_posts", indexes = {
    @Index(name = "idx_company_role", columnList = "normalizedCompany,normalizedRole"),
    @Index(name = "idx_source", columnList = "source"),
    @Index(name = "idx_created_at", columnList = "createdAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Raw identifiers
    @Column(nullable = false)
    private String externalId;          // Reddit post ID, Glassdoor review ID, etc.

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Source source;              // REDDIT, GLASSDOOR, USER_SUBMITTED

    // Normalized search fields (key for retrieval)
    @Column(nullable = false)
    private String normalizedCompany;   // "american express"

    @Column(nullable = false)
    private String normalizedRole;      // "software engineer 1"

    // Raw content
    @Column(nullable = false)
    private String companyRaw;          // "Amex"
    private String roleRaw;             // "SDE 1"

    @Column(columnDefinition = "TEXT", nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String body;

    @Column(columnDefinition = "TEXT")
    private String url;

    // Metadata
    private String author;
    private Integer upvotes;
    private Integer commentCount;
    private String subreddit;
    private Instant createdAt;
    private Instant ingestedAt;

    // Quality signals
    private Boolean isVerified;         // manually verified
    private Double qualityScore;        // computed: upvotes + length + keywords

    // Parsed fields (populated by ingestion pipeline)
    private String detectedRounds;      // JSON array string
    private String detectedTopics;      // JSON array string

    // Location
    private String location;            // "India", "US", "Remote"

    public enum Source {
        REDDIT, GLASSDOOR, LEETCODE_DISCUSS, USER_SUBMITTED
    }
}
