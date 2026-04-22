package com.interviewintel.repository;

import com.interviewintel.model.SearchResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface SearchResultRepository extends JpaRepository<SearchResult, Long> {

    Optional<SearchResult> findByCacheKey(String cacheKey);

    boolean existsByCacheKey(String cacheKey);

    @Modifying
    @Query("UPDATE SearchResult s SET s.totalSearches = s.totalSearches + 1 WHERE s.cacheKey = :key")
    void incrementSearchCount(@Param("key") String key);

    @Query("SELECT s FROM SearchResult s WHERE s.expiresAt < :now")
    java.util.List<SearchResult> findExpired(@Param("now") Instant now);
}
