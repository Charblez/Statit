/**
 * Filename: LeaderboardEntry.java
 * Author: Charles Bassani
 * Description: DTO for leaderboard entries
 */

//----------------------------------------------------------------------------------------------------
// Package
//----------------------------------------------------------------------------------------------------
package com.statit.backend.dto;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

//----------------------------------------------------------------------------------------------------
// Record Definition
//----------------------------------------------------------------------------------------------------
public record LeaderboardEntry(
        int rank,
        UUID scoreId,
        String username,
        Double score,
        Map<String, String> tags,
        Boolean anonymous,
        LocalDateTime submittedAt
) {}
