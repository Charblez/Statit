/**
 * Filename: .java
 * Author:
 * Description:
 */

//----------------------------------------------------------------------------------------------------
// Package
//----------------------------------------------------------------------------------------------------
package com.statit.backend.dto;

import com.statit.backend.model.User;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

//----------------------------------------------------------------------------------------------------
// Record Definition
//----------------------------------------------------------------------------------------------------
public record UserResponse(UUID userId,
                           String username,
                           String email,
                           LocalDate birthday,
                           Map<String, String> demographics,
                           LocalDateTime createdAt,
                           Boolean admin,
                           String message)
{
    public static UserResponse fromUser(User user, String message)
    {
        return new UserResponse(
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getBirthday(),
                user.getDemographics(),
                user.getCreatedAt(),
                user.getAdmin(),
                message
        );
    }
}
