/**
 * Filename: UserController.java
 * Author: Charles Bassani
 * Description: API controller for user CRUD operations.
 */

//----------------------------------------------------------------------------------------------------
// Package
//----------------------------------------------------------------------------------------------------
package com.statit.backend.controller;

//----------------------------------------------------------------------------------------------------
// Imports
//----------------------------------------------------------------------------------------------------
import com.statit.backend.dto.UserCreateRequest;
import com.statit.backend.dto.UserLoginRequest;
import com.statit.backend.dto.UserResponse;
import com.statit.backend.model.User;
import com.statit.backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

//----------------------------------------------------------------------------------------------------
// Class Definition
//----------------------------------------------------------------------------------------------------
@RestController
@RequestMapping("/api/v1/users")
public class UserController
{
    //------------------------------------------------------------------------------------------------
    // Constructors
    //------------------------------------------------------------------------------------------------
    public UserController(UserService userService)
    {
        this.userService = userService;
    }

    //------------------------------------------------------------------------------------------------
    // Public Methods
    //------------------------------------------------------------------------------------------------
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@RequestBody UserCreateRequest request)
    {
        User newUser = userService.createUser(
                request.username(),
                request.email(),
                request.passwordHash(),
                request.birthday(),
                request.demographics()
        );

        UserResponse userResponse = UserResponse.fromUser(newUser, "User created successfully");
        return ResponseEntity.ok(userResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(@RequestBody UserLoginRequest request)
    {
        User user = userService.login(request.username(), request.password());
        UserResponse response = UserResponse.fromUser(user, "Login successful");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{username}")
    public ResponseEntity<UserResponse> getUserByUsername(@PathVariable String username)
    {
        User user = userService.getUser(username);
        UserResponse response = UserResponse.fromUser(user, null);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers()
    {
        List<User> users = userService.getAllUsers();
        List<UserResponse> responses = new ArrayList<>();

        for(User user : users)
        {
            responses.add(UserResponse.fromUser(user, null));
        }

        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{userId}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable UUID userId,
                                                   @RequestBody UserCreateRequest request)
    {
        User updatedUser = userService.updateUser(
                userId,
                request.username(),
                request.email(),
                request.passwordHash(),
                request.birthday(),
                request.demographics()
        );

        UserResponse response = UserResponse.fromUser(updatedUser, "User updated successfully");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<UserResponse> deleteUser(@PathVariable UUID userId)
    {
        User user = userService.getUser(userId);
        String username = user.getUsername();
        userService.deleteUser(userId);

        return ResponseEntity.ok(new UserResponse(
                userId, username, null, null, null, null, false,
                "User deleted successfully"
        ));
    }

    //------------------------------------------------------------------------------------------------
    // Private Variables
    //------------------------------------------------------------------------------------------------
    private final UserService userService;
}
