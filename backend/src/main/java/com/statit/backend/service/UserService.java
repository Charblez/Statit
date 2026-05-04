/**
 * Filename: UserService.java
 * Author: Wilson Jimenez
 * Description: Handles CRUD operations for users and deletes user scores through ScoreService
 */

//----------------------------------------------------------------------------------------------------
// Package
//----------------------------------------------------------------------------------------------------
package com.statit.backend.service;

//----------------------------------------------------------------------------------------------------
// Imports
//----------------------------------------------------------------------------------------------------
import com.statit.backend.model.Score;
import com.statit.backend.model.User;
import com.statit.backend.repository.ScoreRepository;
import com.statit.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

//----------------------------------------------------------------------------------------------------
// Class Definition
//----------------------------------------------------------------------------------------------------
@Service
public class UserService
{
    //------------------------------------------------------------------------------------------------
    // Constructors
    //------------------------------------------------------------------------------------------------
    public UserService(UserRepository userRepository,
                       ScoreRepository scoreRepository,
                       ScoreService scoreService)
    {
        this.userRepository = userRepository;
        this.scoreRepository = scoreRepository;
        this.scoreService = scoreService;
    }

    //------------------------------------------------------------------------------------------------
    // Public Methods
    //------------------------------------------------------------------------------------------------
    @Transactional
    public User createUser(String username,
                           String email,
                           String passwordHash,
                           LocalDate birthday,
                           Map<String, String> demographics)
    {
        //Check if username exists already
        if(userRepository.findByUsername(username).isPresent())
        {
            throw new IllegalArgumentException("Username already exists.");
        }

        //Check if email exists already
        if(userRepository.findByEmail(email).isPresent())
        {
            throw new IllegalArgumentException("Email already exists.");
        }

        //Create and save the user
        User user = new User(username, email, passwordHash, birthday, demographics);
        return userRepository.save(user);
    }

    public User getUser(UUID userId)
    {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
    }

    public User getUser(String username)
    {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
    }

    public List<User> getAllUsers()
    {
        return userRepository.findAll();
    }

    @Transactional
    public User updateUser(UUID userId,
                           String username,
                           String email,
                           String passwordHash,
                           LocalDate birthday,
                           Map<String, String> demographics)
    {
        //Fetch existing user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        //Check if username conflicts with another user
        Optional<User> userWithUsername = userRepository.findByUsername(username);
        if(userWithUsername.isPresent() && !userWithUsername.get().getUserId().equals(userId))
        {
            throw new IllegalArgumentException("Username already exists.");
        }

        //Check if email conflicts with another user
        Optional<User> userWithEmail = userRepository.findByEmail(email);
        if(userWithEmail.isPresent() && !userWithEmail.get().getUserId().equals(userId))
        {
            throw new IllegalArgumentException("Email already exists.");
        }

        //Update mutable fields only 
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordHash);
        user.setBirthday(birthday);
        user.setDemographics(demographics != null ? demographics : new HashMap<>());
        return userRepository.save(user);
    }

    @Transactional
    public User grantAdmin(String username)
    {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
        user.setAdmin(true);
        return userRepository.save(user);
    }

    public List<User> searchUsers(String query)
    {
        if(query == null || query.isBlank()) return userRepository.findAll();
        String needle = query.toLowerCase();
        List<User> all = userRepository.findAll();
        List<User> matches = new ArrayList<>();
        for(User u : all)
        {
            if(u.getUsername() != null && u.getUsername().toLowerCase().contains(needle)) matches.add(u);
        }
        return matches;
    }

    @Transactional
    public void deleteUser(UUID userId)
    {
        //Get the user to delete
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        //Collect all score IDs before deleting to avoid list mutation issues
        List<Score> userScores = scoreRepository.findAllByUser(user);
        List<UUID> scoreIds = new ArrayList<>();

        for(Score score : userScores)
        {
            scoreIds.add(score.getScoreId());
        }

        //Delete each score through ScoreService so baselines are kept consistent
        for(UUID scoreId : scoreIds)
        {
            scoreService.deleteScore(scoreId);
        }

        //Delete the user
        userRepository.delete(user);
    }

    //------------------------------------------------------------------------------------------------
    // Private Variables
    //------------------------------------------------------------------------------------------------
    private final UserRepository userRepository;
    private final ScoreRepository scoreRepository;
    private final ScoreService scoreService;
}
