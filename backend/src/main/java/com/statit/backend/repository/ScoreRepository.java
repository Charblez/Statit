/**
 * Filename: ScoreRepository.java
 * Author: Charles Bassani
 * Description: Repository for Score table queries
 */

//----------------------------------------------------------------------------------------------------
// Package
//----------------------------------------------------------------------------------------------------
package com.statit.backend.repository;

//----------------------------------------------------------------------------------------------------
// Imports
//----------------------------------------------------------------------------------------------------
import com.statit.backend.model.Category;
import com.statit.backend.model.Score;
import com.statit.backend.model.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

//----------------------------------------------------------------------------------------------------
// Interface Definition
//----------------------------------------------------------------------------------------------------
@Repository
public interface ScoreRepository extends JpaRepository<Score, UUID>
{
    interface CorrelationPointProjection
    {
        UUID getUserId();
        Double getPrimaryScore();
        Double getSecondaryScore();
    }

    //------------------------------------------------------------------------------------------------
    // Single Score Lookups
    //------------------------------------------------------------------------------------------------
    Optional<Score> findFirstByCategoryAndUserOrderByScoreDesc(Category category, User user);

    Optional<Score> findFirstByCategoryAndUserOrderByScoreAsc(Category category, User user);

    //------------------------------------------------------------------------------------------------
    // User History and Counts
    //------------------------------------------------------------------------------------------------
    List<Score> findAllByUser(User user);

    Page<Score> findByUserOrderBySubmittedAtDesc(User user, Pageable pageable);

    long countByCategoryAndRejectedFalse(Category category);

    //------------------------------------------------------------------------------------------------
    // Bulk Deletes
    //------------------------------------------------------------------------------------------------
    void deleteAllByCategory(Category category);

    //------------------------------------------------------------------------------------------------
    // Paginated Leaderboard Queries
    //------------------------------------------------------------------------------------------------
    @Query(value = "SELECT * FROM (" +
            "SELECT DISTINCT ON (s.user_id) s.* FROM scores s " +
            "WHERE s.category_id = :categoryId AND s.rejected = false " +
            "ORDER BY s.user_id, s.score_value DESC" +
            ") best ORDER BY best.score_value DESC",
            countQuery = "SELECT COUNT(DISTINCT s.user_id) FROM scores s " +
                    "WHERE s.category_id = :categoryId AND s.rejected = false",
            nativeQuery = true)
    Page<Score> findTopScoresPerUserDesc(@Param("categoryId") UUID categoryId, Pageable pageable);

    @Query(value = "SELECT * FROM (" +
            "SELECT DISTINCT ON (s.user_id) s.* FROM scores s " +
            "WHERE s.category_id = :categoryId AND s.rejected = false " +
            "ORDER BY s.user_id, s.score_value ASC" +
            ") best ORDER BY best.score_value ASC",
            countQuery = "SELECT COUNT(DISTINCT s.user_id) FROM scores s " +
                    "WHERE s.category_id = :categoryId AND s.rejected = false",
            nativeQuery = true)
    Page<Score> findTopScoresPerUserAsc(@Param("categoryId") UUID categoryId, Pageable pageable);

    //------------------------------------------------------------------------------------------------
    // Rank Computation Queries
    //------------------------------------------------------------------------------------------------
    @Query(value = "SELECT COUNT(DISTINCT s.user_id) FROM scores s " +
            "WHERE s.category_id = :categoryId AND s.rejected = false",
            nativeQuery = true)
    long countDistinctUsersByCategoryAndRejectedFalse(@Param("categoryId") UUID categoryId);

    @Query(value = "SELECT COUNT(*) FROM (" +
            "SELECT DISTINCT ON (s.user_id) s.score_value FROM scores s " +
            "WHERE s.category_id = :categoryId AND s.rejected = false " +
            "ORDER BY s.user_id, s.score_value DESC" +
            ") best WHERE best.score_value > :scoreValue",
            nativeQuery = true)
    long countUsersWithBetterScoreDesc(@Param("categoryId") UUID categoryId,
                                       @Param("scoreValue") Double scoreValue);

    default long countUsersWithBetterScoreDesc(UUID categoryId, Float scoreValue)
    {
        return countUsersWithBetterScoreDesc(categoryId, scoreValue != null ? scoreValue.doubleValue() : null);
    }

    @Query(value = "SELECT COUNT(*) FROM (" +
            "SELECT DISTINCT ON (s.user_id) s.score_value FROM scores s " +
            "WHERE s.category_id = :categoryId AND s.rejected = false " +
            "ORDER BY s.user_id, s.score_value ASC" +
            ") best WHERE best.score_value < :scoreValue",
            nativeQuery = true)
    long countUsersWithBetterScoreAsc(@Param("categoryId") UUID categoryId,
                                      @Param("scoreValue") Double scoreValue);

    default long countUsersWithBetterScoreAsc(UUID categoryId, Float scoreValue)
    {
        return countUsersWithBetterScoreAsc(categoryId, scoreValue != null ? scoreValue.doubleValue() : null);
    }

    //------------------------------------------------------------------------------------------------
    // Filtered Leaderboards
    //------------------------------------------------------------------------------------------------
    @Query(value = "SELECT * FROM (" +
            "SELECT DISTINCT ON (s.user_id) s.* FROM scores s " +
            "WHERE s.category_id = :categoryId AND s.rejected = false " +
            "AND s.tags @> CAST(:tags AS jsonb) " +
            "ORDER BY s.user_id, s.score_value DESC" +
            ") best ORDER BY best.score_value DESC",
            countQuery = "SELECT COUNT(DISTINCT s.user_id) FROM scores s " +
                    "WHERE s.category_id = :categoryId AND s.rejected = false " +
                    "AND s.tags @> CAST(:tags AS jsonb)",
            nativeQuery = true)
    Page<Score> findFilteredTopScoresPerUserDesc(@Param("categoryId") UUID categoryId,
                                                 @Param("tags") String tagsJson,
                                                 Pageable pageable);

    @Query(value = "SELECT * FROM (" +
            "SELECT DISTINCT ON (s.user_id) s.* FROM scores s " +
            "WHERE s.category_id = :categoryId AND s.rejected = false " +
            "AND s.tags @> CAST(:tags AS jsonb) " +
            "ORDER BY s.user_id, s.score_value ASC" +
            ") best ORDER BY best.score_value ASC",
            countQuery = "SELECT COUNT(DISTINCT s.user_id) FROM scores s " +
                    "WHERE s.category_id = :categoryId AND s.rejected = false " +
                    "AND s.tags @> CAST(:tags AS jsonb)",
            nativeQuery = true)
    Page<Score> findFilteredTopScoresPerUserAsc(@Param("categoryId") UUID categoryId,
                                                @Param("tags") String tagsJson,
                                                Pageable pageable);

    //------------------------------------------------------------------------------------------------
    // Correlation Queries
    //------------------------------------------------------------------------------------------------
    @Query(value = """
            WITH primary_scores AS (
                SELECT
                    s.user_id,
                    s.score_value AS primary_score,
                    ROW_NUMBER() OVER (
                        PARTITION BY s.user_id
                        ORDER BY
                            CASE WHEN :primaryDescending = true THEN s.score_value END DESC,
                            CASE WHEN :primaryDescending = false THEN s.score_value END ASC,
                            s.submitted_at DESC
                    ) AS rn
                FROM scores s
                WHERE s.category_id = :primaryCategoryId
                  AND s.rejected = false
            ),
            secondary_scores AS (
                SELECT
                    s.user_id,
                    s.score_value AS secondary_score,
                    ROW_NUMBER() OVER (
                        PARTITION BY s.user_id
                        ORDER BY
                            CASE WHEN :secondaryDescending = true THEN s.score_value END DESC,
                            CASE WHEN :secondaryDescending = false THEN s.score_value END ASC,
                            s.submitted_at DESC
                    ) AS rn
                FROM scores s
                WHERE s.category_id = :secondaryCategoryId
                  AND s.rejected = false
            )
            SELECT
                p.user_id AS "userId",
                p.primary_score AS "primaryScore",
                s.secondary_score AS "secondaryScore"
            FROM primary_scores p
            INNER JOIN secondary_scores s ON p.user_id = s.user_id
            WHERE p.rn = 1
              AND s.rn = 1
            ORDER BY p.primary_score ASC
            """,
            nativeQuery = true)
    List<CorrelationPointProjection> findPairedTopScoresForCorrelation(
            @Param("primaryCategoryId") UUID primaryCategoryId,
            @Param("secondaryCategoryId") UUID secondaryCategoryId,
            @Param("primaryDescending") boolean primaryDescending,
            @Param("secondaryDescending") boolean secondaryDescending);

}
