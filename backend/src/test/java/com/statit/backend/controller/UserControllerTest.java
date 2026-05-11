package com.statit.backend.controller;

import com.statit.backend.TestUtils;
import com.statit.backend.dto.UserCreateRequest;
import com.statit.backend.dto.UserLoginRequest;
import com.statit.backend.dto.UserResponse;
import com.statit.backend.model.User;
import com.statit.backend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest
{
    @Mock private UserService userService;
    @InjectMocks private UserController userController;

    private User user;
    private UUID userId;

    @BeforeEach
    void setUp()
    {
        userId = UUID.randomUUID();
        user = new User("alice", "a@x", "h", LocalDate.of(2000, 1, 1), null);
        TestUtils.setField(user, "userId", userId);
    }

    @Test
    void createUserReturnsOk()
    {
        UserCreateRequest req = new UserCreateRequest("alice", "a@x", "h", LocalDate.of(2000, 1, 1), null);
        when(userService.createUser(any(), any(), any(), any(), any())).thenReturn(user);

        ResponseEntity<UserResponse> response = userController.createUser(req);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("alice", response.getBody().username());
        assertEquals("User created successfully", response.getBody().message());
    }

    @Test
    void getUserByUsernameReturnsOk()
    {
        when(userService.getUser("alice")).thenReturn(user);
        ResponseEntity<UserResponse> response = userController.getUserByUsername("alice");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("alice", response.getBody().username());
    }

    @Test
    void loginReturnsOk()
    {
        UserLoginRequest req = new UserLoginRequest("alice", "h");
        when(userService.login("alice", "h")).thenReturn(user);

        ResponseEntity<UserResponse> response = userController.login(req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("alice", response.getBody().username());
        assertEquals("Login successful", response.getBody().message());
    }

    @Test
    void getAllUsersReturnsList()
    {
        User u2 = new User("bob", "b@x", "h", LocalDate.of(2001, 1, 1), null);
        TestUtils.setField(u2, "userId", UUID.randomUUID());
        when(userService.getAllUsers()).thenReturn(Arrays.asList(user, u2));

        ResponseEntity<List<UserResponse>> response = userController.getAllUsers();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().size());
    }

    @Test
    void updateUserReturnsOkWithMessage()
    {
        UserCreateRequest req = new UserCreateRequest("alice2", "a2@x", "h2",
                LocalDate.of(2000, 1, 1), null);
        when(userService.updateUser(any(), any(), any(), any(), any(), any())).thenReturn(user);

        ResponseEntity<UserResponse> response = userController.updateUser(userId, req);
        assertEquals("User updated successfully", response.getBody().message());
    }

    @Test
    void deleteUserReturnsOkWithDeletedMessage()
    {
        when(userService.getUser(userId)).thenReturn(user);

        ResponseEntity<UserResponse> response = userController.deleteUser(userId);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("alice", response.getBody().username());
        assertEquals("User deleted successfully", response.getBody().message());
        verify(userService).deleteUser(userId);
    }
}
