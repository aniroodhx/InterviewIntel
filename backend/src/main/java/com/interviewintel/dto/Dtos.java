package com.interviewintel.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

//Inbound

public class Dtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchRequest {
        private String company;         // raw: "Amex", "AMEX", "american express"
        private String role;            // raw: "SDE1", "sde 1", "Software Engineer"
        private String location;        // "IN" or "US"
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IngestRequest {
        private String company;
        private String role;
        private String location;        // optional filter
    }

    //Outbound

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class InterviewIntelResponse {
        private String company;
        private String role;
        private String location;
        private String difficulty;
        private Integer difficultyScore;        // 1-5
        private String overview;
        private String collectiveExperience;
        private List<String> rounds;
        private List<String> topics;
        private CompensationDto compensation;
        private List<QuestionDto> questions;
        private List<TopicFrequency> topicFrequencies;
        private List<String> tips;
        private List<SourceRef> sources;
        private Integer sourcePostCount;
        private Boolean isCached;
        private String generatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CompensationDto {
        private String currency;
        private String base;
        private String totalComp;
        private String bonus;
        private String equity;
        private String note;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class QuestionDto {
        private String category;        // DSA, System Design, Behavioral, CS Fundamentals
        private String question;
        private String difficulty;      // Easy, Medium, Hard
        private Integer frequency;      // how many posts mentioned this type
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopicFrequency {
        private String topic;
        private Integer count;
        private Double percentage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SourceRef {
        private String platform;
        private String url;
        private String title;
        private Integer upvotes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IngestResponse {
        private String status;
        private Integer postsIngested;
        private String message;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorResponse {
        private String error;
        private String message;
        private Integer status;
    }
}
