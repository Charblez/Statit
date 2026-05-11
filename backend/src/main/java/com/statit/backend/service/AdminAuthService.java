/**
 * Filename: AdminAuthService.java
 * Description: Lightweight admin authentication helper for admin-only endpoints.
 */

package com.statit.backend.service;

import com.statit.backend.model.User;
import com.statit.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminAuthService
{
    public AdminAuthService(UserRepository userRepository)
    {
        this.userRepository = userRepository;
    }

    public User requireAdmin(String username)
    {
        if(username == null || username.isBlank())
        {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Admin credentials required.");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Admin credentials required."));

        if(!Boolean.TRUE.equals(user.getAdmin()))
        {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin privileges required.");
        }

        return user;
    }

    private final UserRepository userRepository;
}
