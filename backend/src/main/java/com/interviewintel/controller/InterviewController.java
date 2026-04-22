package com.interviewintel.controller;

import com.interviewintel.dto.Dtos;
import com.interviewintel.ingestion.RedditIngestionService;
import com.interviewintel.service.QueryNormalizer;
import com.interviewintel.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "${cors.allowed.origins}")
public class InterviewController {

    private final SearchService searchService;
    private final RedditIngestionService redditIngestionService;
    private final QueryNormalizer normalizer;

    /**
     * GET /api/v1/search?company=Amex&role=SDE1&location=IN
     *
     * Primary search endpoint. Returns full structured intelligence.
     */
    @GetMapping("/search")
    public ResponseEntity<?> search(
        @RequestParam String company,
        @RequestParam String role,
        @RequestParam(defaultValue = "IN") String location
    ) {
        if (company == null || company.isBlank() || role == null || role.isBlank()) {
            return ResponseEntity.badRequest().body(
                Dtos.ErrorResponse.builder()
                    .error("INVALID_REQUEST")
                    .message("company and role are required")
                    .status(400)
                    .build()
            );
        }

        try {
            Dtos.SearchRequest req = Dtos.SearchRequest.builder()
                .company(company.trim())
                .role(role.trim())
                .location(location.toUpperCase())
                .build();

            Dtos.InterviewIntelResponse response = searchService.search(req);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Search failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                Dtos.ErrorResponse.builder()
                    .error("SEARCH_FAILED")
                    .message("Search failed. Please try again.")
                    .status(500)
                    .build()
            );
        }
    }

    /**
     * POST /api/v1/ingest
     *
     * Triggers Reddit data ingestion for a company+role.
     * Can be called manually or via a scheduled job.
     */
    @PostMapping("/ingest")
    public ResponseEntity<Dtos.IngestResponse> ingest(@RequestBody Dtos.IngestRequest request) {
        log.info("Manual ingest triggered: {}/{}", request.getCompany(), request.getRole());
        redditIngestionService.ingestForCompanyRole(
            request.getCompany(),
            request.getRole(),
            request.getLocation()
        );
        return ResponseEntity.accepted().body(
            Dtos.IngestResponse.builder()
                .status("ACCEPTED")
                .message("Ingestion started in background. Check back in 30 seconds.")
                .postsIngested(0)
                .build()
        );
    }

    /**
     * GET /api/v1/autocomplete/companies
     */
    @GetMapping("/autocomplete/companies")
    public ResponseEntity<List<String>> autocompleteCompanies() {
        return ResponseEntity.ok(searchService.autocompleteCompanies());
    }

    /**
     * GET /api/v1/autocomplete/roles?company=Amex
     */
    @GetMapping("/autocomplete/roles")
    public ResponseEntity<List<String>> autocompleteRoles(@RequestParam String company) {
        return ResponseEntity.ok(searchService.autocompleteRoles(company));
    }

    /**
     * GET /api/v1/normalize?company=Amex&role=SDE1
     * Debug endpoint to test normalization.
     */
    @GetMapping("/normalize")
    public ResponseEntity<Map<String, String>> normalize(
        @RequestParam String company,
        @RequestParam String role
    ) {
        return ResponseEntity.ok(Map.of(
            "normalizedCompany", normalizer.normalizeCompany(company),
            "normalizedRole", normalizer.normalizeRole(role),
            "displayCompany", normalizer.toDisplayName(normalizer.normalizeCompany(company)),
            "cacheKey", normalizer.buildCacheKey(company, role, "IN")
        ));
    }

    /**
     * GET /api/v1/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "InterviewIntel"));
    }
}
