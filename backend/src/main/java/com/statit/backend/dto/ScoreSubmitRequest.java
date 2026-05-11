/**
 * Filename: ScoreSubmitRequest.java
 * Author:
 * Description: DTO for score submission payloads.
 */

//----------------------------------------------------------------------------------------------------
// Package
//----------------------------------------------------------------------------------------------------
package com.statit.backend.dto;

//----------------------------------------------------------------------------------------------------
// Imports
//----------------------------------------------------------------------------------------------------
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;
import java.util.UUID;

//----------------------------------------------------------------------------------------------------
// Record Definition
//----------------------------------------------------------------------------------------------------
public record ScoreSubmitRequest(@JsonProperty("user_id") UUID userId,
                                 @JsonProperty("category_id") UUID categoryId,
                                 Double score,
                                 Map<String, String> tags,
                                 Boolean anonymous)
{
    public ScoreSubmitRequest(UUID userId,
                              UUID categoryId,
                              Float score,
                              Map<String, String> tags,
                              Boolean anonymous)
    {
        this(userId, categoryId, score != null ? score.doubleValue() : null, tags, anonymous);
    }
}
