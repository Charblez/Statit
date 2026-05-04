/**
 * Filename: ScoreInfoResponse.java
 * Author: Charles Bassani
 * Description: DTO for detailed score information including rank and percentile.
 */

//----------------------------------------------------------------------------------------------------
// Package
//----------------------------------------------------------------------------------------------------
package com.statit.backend.dto;

//----------------------------------------------------------------------------------------------------
// Imports
//----------------------------------------------------------------------------------------------------
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

//----------------------------------------------------------------------------------------------------
// Record Definition
//----------------------------------------------------------------------------------------------------
public record ScoreInfoResponse(
        UUID scoreId,
        Double score,
        String categoryName,
        UUID categoryId,
        String units,
        String username,
        UUID userId,
        Boolean anonymous,
        Map<String, String> tags,
        LocalDateTime submittedAt,
        int rank,
        long totalParticipants,
        Double percentile,
        Double zScore,
        Float baselineMean,
        Float baselineStdDev,
        Integer baselineSampleSize
) {}
