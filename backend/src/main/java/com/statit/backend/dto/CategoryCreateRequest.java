/**
 * Filename: CategoryCreateRequest.java
 * Author: Wilson Jimenez
 * Description: DTO for category creation payloads.
 */

//----------------------------------------------------------------------------------------------------
// Package
//----------------------------------------------------------------------------------------------------
package com.statit.backend.dto;

//----------------------------------------------------------------------------------------------------
// Imports
//----------------------------------------------------------------------------------------------------
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.UUID;

//----------------------------------------------------------------------------------------------------
// Record Definition
//----------------------------------------------------------------------------------------------------
public record CategoryCreateRequest(String name,
                                    String description,
                                    String units,
                                    List<String> tags,
                                    @JsonProperty("sort_order") Boolean sortOrder,
                                    @JsonProperty("founding_username") String foundingUsername,
                                    @JsonProperty("lower_limit") Double lowerLimit,
                                    @JsonProperty("upper_limit") Double upperLimit,
                                    @JsonProperty("image_data") String imageData)
{
    public CategoryCreateRequest(String name,
                                 String description,
                                 String units,
                                 List<String> tags,
                                 Boolean sortOrder,
                                 String foundingUsername)
    {
        this(name, description, units, tags, sortOrder, foundingUsername, 0.0, 100.0, null);
    }
}
