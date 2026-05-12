package com.statit.backend.service;

import com.statit.backend.TestUtils;
import com.statit.backend.model.Category;
import com.statit.backend.model.Score;
import com.statit.backend.model.User;
import com.statit.backend.repository.ScoreRepository;
import com.statit.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest
{
    @Mock private UserRepository userRepository;
    @Mock private ScoreRepository scoreRepository;
    @Mock private ScoreService scoreService;

    @InjectMocks private UserService userService;

    private User existing;
    private UUID userId;

    @BeforeEach
    void setUp()
    {
        userId = UUID.randomUUID();
        existing = new User("alice", "a@x.com", "h", LocalDate.of(2000, 1, 1), null);
        TestUtils.setField(existing, "userId", userId);
    }

    @Test
    void createUserSavesNewUser()
    {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("a@x.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.createUser("alice", "a@x.com", "hash", LocalDate.of(2000, 1, 1), null);

        assertEquals("alice", result.getUsername());
        assertNotEquals("hash", result.getPasswordHash());
        assertTrue(result.getPasswordHash().startsWith("pbkdf2_sha256$"));
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUserRejectsDuplicateUsername()
    {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(existing));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.createUser("alice", "x@y", "h", LocalDate.now(), null));
        assertTrue(ex.getMessage().contains("Username"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUserRejectsDuplicateEmail()
    {
        when(userRepository.findByUsername("new")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("a@x.com")).thenReturn(Optional.of(existing));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.createUser("new", "a@x.com", "h", LocalDate.now(), null));
        assertTrue(ex.getMessage().contains("Email"));
    }

    @Test
    void getUserByIdReturnsUser()
    {
        when(userRepository.findById(userId)).thenReturn(Optional.of(existing));
        assertSame(existing, userService.getUser(userId));
    }

    @Test
    void getUserByIdMissingThrows()
    {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> userService.getUser(id));
    }

    @Test
    void getUserByUsernameReturnsUser()
    {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(existing));
        assertSame(existing, userService.getUser("alice"));
    }

    @Test
    void getUserByUsernameMissingThrows()
    {
        when(userRepository.findByUsername("nope")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> userService.getUser("nope"));
    }

    @Test
    void loginAcceptsMatchingPasswordAndUpgradesLegacyPlaintextPassword()
    {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.login("alice", "h");

        assertSame(existing, result);
        assertNotEquals("h", result.getPasswordHash());
        assertTrue(result.getPasswordHash().startsWith("pbkdf2_sha256$"));
        verify(userRepository).save(existing);
    }

    @Test
    void loginRejectsWrongPassword()
    {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(existing));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.login("alice", "wrong"));

        assertTrue(ex.getMessage().contains("Invalid username or password"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void getAllUsersReturnsRepoList()
    {
        List<User> users = Arrays.asList(existing);
        when(userRepository.findAll()).thenReturn(users);
        assertEquals(users, userService.getAllUsers());
    }

    @Test
    void updateUserPersistsChanges()
    {
        when(userRepository.findById(userId)).thenReturn(Optional.of(existing));
        when(userRepository.findByUsername("alice2")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("a2@x")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.updateUser(userId, "alice2", "a2@x", "h2",
                LocalDate.of(2001, 2, 2), new HashMap<>());

        assertEquals("alice2", result.getUsername());
        assertEquals("a2@x", result.getEmail());
        assertNotEquals("h2", result.getPasswordHash());
        assertTrue(result.getPasswordHash().startsWith("pbkdf2_sha256$"));
    }

    @Test
    void updateUserAllowsKeepingOwnUsername()
    {
        when(userRepository.findById(userId)).thenReturn(Optional.of(existing));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(existing));
        when(userRepository.findByEmail("a@x.com")).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.updateUser(userId, "alice", "a@x.com", "h",
                LocalDate.of(2000, 1, 1), null);
        assertSame(existing, result);
    }

    @Test
    void updateUserRejectsUsernameTakenByAnother()
    {
        User other = new User("alice2", "x@y", "h", LocalDate.now(), null);
        TestUtils.setField(other, "userId", UUID.randomUUID());

        when(userRepository.findById(userId)).thenReturn(Optional.of(existing));
        when(userRepository.findByUsername("alice2")).thenReturn(Optional.of(other));

        assertThrows(IllegalArgumentException.class, () ->
                userService.updateUser(userId, "alice2", "a@x.com", "h", LocalDate.now(), null));
    }

    @Test
    void updateUserRejectsEmailTakenByAnother()
    {
        User other = new User("other", "taken@x", "h", LocalDate.now(), null);
        TestUtils.setField(other, "userId", UUID.randomUUID());

        when(userRepository.findById(userId)).thenReturn(Optional.of(existing));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("taken@x")).thenReturn(Optional.of(other));

        assertThrows(IllegalArgumentException.class, () ->
                userService.updateUser(userId, "alice", "taken@x", "h", LocalDate.now(), null));
    }

    @Test
    void updateUserMissingThrows()
    {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () ->
                userService.updateUser(userId, "x", "y", "z", LocalDate.now(), null));
    }

    @Test
    void deleteUserDeletesScoresThenUser()
    {
        Category category = new Category("c", "d", "u", null, true, existing);
        Score s1 = new Score(category, existing, 1f, null, false);
        Score s2 = new Score(category, existing, 2f, null, false);
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        TestUtils.setField(s1, "scoreId", id1);
        TestUtils.setField(s2, "scoreId", id2);

        when(userRepository.findById(userId)).thenReturn(Optional.of(existing));
        when(scoreRepository.findAllByUser(existing)).thenReturn(Arrays.asList(s1, s2));

        userService.deleteUser(userId);

        verify(scoreService).deleteScore(id1);
        verify(scoreService).deleteScore(id2);
        verify(userRepository).delete(existing);
    }

    @Test
    void deleteUserMissingThrows()
    {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> userService.deleteUser(userId));
        verify(userRepository, never()).delete(any());
    }
}
