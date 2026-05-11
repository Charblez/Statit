/**
 * Filename: ScoreResponse.java
 * Author: Wilson Jimenez
 * Description: DTO for score submission and retrieval responses.
 */

//----------------------------------------------------------------------------------------------------
// Package
//----------------------------------------------------------------------------------------------------
package com.statit.backend.dto;

import com.statit.backend.model.Score;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

//----------------------------------------------------------------------------------------------------
// Record Definition
//----------------------------------------------------------------------------------------------------
public record ScoreResponse(UUID scoreId,
                            UUID categoryId,
                            String categoryName,
                            UUID userId,
                            String username,
                            Double score,
                            Map<String, String> tags,
                            Boolean anonymous,
                            LocalDateTime submittedAt,
                            String message)
{
    public static ScoreResponse fromScore(Score score, String message)
    {
        return new ScoreResponse(
                score.getScoreId(),
                score.getCategory().getCategoryId(),
                score.getCategory().getName(),
                score.getAnonymous() ? null : score.getUser().getUserId(),
                score.getAnonymous() ? "Anonymous" : score.getUser().getUsername(),
                score.getScore(),
                score.getTags(),
                score.getAnonymous(),
                score.getSubmittedAt(),
                message
        );
    }
}
