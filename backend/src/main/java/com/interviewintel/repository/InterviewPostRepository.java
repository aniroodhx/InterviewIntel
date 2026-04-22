package com.interviewintel.repository;

import com.interviewintel.model.InterviewPost;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface InterviewPostRepository extends JpaRepository<InterviewPost, Long> {

    //Existence check 
    boolean existsByExternalIdAndSource(String externalId, InterviewPost.Source source);

    //Primary retrieval: exact match on normalized company + role 
    @Query("""
        SELECT p FROM InterviewPost p
        WHERE p.normalizedCompany = :company
          AND p.normalizedRole = :role
          AND (:location IS NULL OR p.location = :location)
          AND p.qualityScore >= :minQuality
        ORDER BY p.qualityScore DESC, p.upvotes DESC
        """)
    List<InterviewPost> findByCompanyAndRole(
        @Param("company") String company,
        @Param("role") String role,
        @Param("location") String location,
        @Param("minQuality") double minQuality,
        Pageable pageable
    );

    //Fuzzy fallback: company match only, broader role search
    @Query("""
        SELECT p FROM InterviewPost p
        WHERE p.normalizedCompany = :company
          AND (p.normalizedRole LIKE %:roleKeyword% OR :roleKeyword IS NULL)
          AND (:location IS NULL OR p.location = :location)
        ORDER BY p.qualityScore DESC
        """)
    List<InterviewPost> findByCompanyFuzzy(
        @Param("company") String company,
        @Param("roleKeyword") String roleKeyword,
        @Param("location") String location,
        Pageable pageable
    );

    //Full-text style search (PostgreSQL ILIKE)eifjcbrhrlefid
    @Query(value = """
        SELECT * FROM interview_posts
        WHERE normalized_company ILIKE :companyPattern
          AND (normalized_role ILIKE :rolePattern OR :rolePattern IS NULL)
          AND quality_score >= :minQuality
        ORDER BY quality_score DESC, upvotes DESC
        LIMIT :lim
        """, nativeQuery = true)
    List<InterviewPost> fullTextSearch(
        @Param("companyPattern") String companyPattern,
        @Param("rolePattern") String rolePattern,
        @Param("minQuality") double minQuality,
        @Param("lim") int limit
    );

    //Count for a company+role combo 
    long countByNormalizedCompanyAndNormalizedRole(String company, String role);

    //Fetch all distinct companies (for autocomplete)
    @Query("SELECT DISTINCT p.normalizedCompany FROM InterviewPost p ORDER BY p.normalizedCompany")
    List<String> findDistinctCompanies();

    //Fetch distinct roles for a company (for autocomplete)
    @Query("SELECT DISTINCT p.normalizedRole FROM InterviewPost p WHERE p.normalizedCompany = :company")
    List<String> findDistinctRolesForCompany(@Param("company") String company);

    //Most searched companies (by post count)
    @Query("""
        SELECT p.normalizedCompany, COUNT(p) as cnt
        FROM InterviewPost p
        GROUP BY p.normalizedCompany
        ORDER BY cnt DESC
        """)
    List<Object[]> findTopCompaniesByPostCount(Pageable pageable);

    //Stale posts for re-ingestion
    @Query("SELECT p FROM InterviewPost p WHERE p.ingestedAt < :cutoff ORDER BY p.ingestedAt ASC")
    List<InterviewPost> findPostsOlderThan(@Param("cutoff") Instant cutoff, Pageable pageable);
}
