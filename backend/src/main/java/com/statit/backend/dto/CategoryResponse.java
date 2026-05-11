/**
 * Filename: CategoryResponse.java
 * Author: Wilson Jimenez
 * Description: DTO for category responses.
 */

//----------------------------------------------------------------------------------------------------
// Package
//----------------------------------------------------------------------------------------------------
package com.statit.backend.dto;

//----------------------------------------------------------------------------------------------------
// Imports
//----------------------------------------------------------------------------------------------------
import com.fasterxml.jackson.annotation.JsonProperty;
import com.statit.backend.model.Category;
import com.statit.backend.model.CategoryScope;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

//----------------------------------------------------------------------------------------------------
// Record Definition
//----------------------------------------------------------------------------------------------------
public record CategoryResponse(UUID categoryId,
                               String name,
                               String description,
                               String units,
                               List<String> tags,
                               Boolean sortOrder,
                               Double lowerLimit,
                               Double upperLimit,
                               String imageData,
                               CategoryScope categoryScope,
                               String globalSourceKey,
                               @JsonProperty("isGlobal") Boolean isGlobal,
                               LocalDateTime createdAt,
                               Boolean live,
                               String message)
{
    public static CategoryResponse fromCategory(Category category, String message)
    {
        return new CategoryResponse(
                category.getCategoryId(),
                category.getName(),
                category.getDescription(),
                category.getUnits(),
                category.getTags(),
                category.getSortOrder(),
                category.getLowerLimit(),
                category.getUpperLimit(),
                category.getImageData(),
                category.getCategoryScope(),
                category.getGlobalSourceKey(),
                category.isGlobal(),
                category.getCreatedAt(),
                category.getLive(),
                message
        );
    }
}
