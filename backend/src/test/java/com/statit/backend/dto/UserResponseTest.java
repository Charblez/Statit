package com.statit.backend.dto;

import com.statit.backend.TestUtils;
import com.statit.backend.model.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserResponseTest
{
    @Test
    void fromUserCopiesAllFields()
    {
        UUID id = UUID.randomUUID();
        LocalDate birthday = LocalDate.of(2000, 1, 1);
        LocalDateTime createdAt = LocalDateTime.of(2024, 5, 5, 10, 0);
        Map<String, String> demo = new HashMap<>();
        demo.put("country", "US");

        User user = new User("alice", "a@b.com", "hash", birthday, demo);
        TestUtils.setField(user, "userId", id);
        TestUtils.setField(user, "createdAt", createdAt);

        UserResponse response = UserResponse.fromUser(user, "ok");

        assertEquals(id, response.userId());
        assertEquals("alice", response.username());
        assertEquals("a@b.com", response.email());
        assertEquals(birthday, response.birthday());
        assertEquals("US", response.demographics().get("country"));
        assertEquals(createdAt, response.createdAt());
        assertEquals("ok", response.message());
    }

    @Test
    void recordHonorsExplicitConstructor()
    {
        UUID id = UUID.randomUUID();
        UserResponse response = new UserResponse(id, "bob", null, null, null, null, false, "deleted");
        assertEquals(id, response.userId());
        assertEquals("bob", response.username());
        assertEquals("deleted", response.message());
        assertNull(response.email());
    }
}
