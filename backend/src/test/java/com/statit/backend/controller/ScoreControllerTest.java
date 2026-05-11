package com.statit.backend.controller;

import com.statit.backend.TestUtils;
import com.statit.backend.dto.ScoreInfoResponse;
import com.statit.backend.dto.ScoreResponse;
import com.statit.backend.dto.ScoreSubmitRequest;
import com.statit.backend.model.Category;
import com.statit.backend.model.Score;
import com.statit.backend.model.User;
import com.statit.backend.service.ScoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScoreControllerTest
{
    @Mock private ScoreService scoreService;
    @InjectMocks private ScoreController scoreController;

    private User user;
    private Category category;
    private Score score;

    @BeforeEach
    void setUp()
    {
        user = new User("alice", "a@x", "h", LocalDate.of(2000, 1, 1), null);
        TestUtils.setField(user, "userId", UUID.randomUUID());
        category = new Category("Cat", "d", "u", null, true, user);
        TestUtils.setField(category, "categoryId", UUID.randomUUID());
        score = new Score(category, user, 10f, new HashMap<>(), false);
        TestUtils.setField(score, "scoreId", UUID.randomUUID());
    }

    @Test
    void submitScoreReturnsOk()
    {
        ScoreSubmitRequest req = new ScoreSubmitRequest(user.getUserId(), category.getCategoryId(),
                10f, null, false);
        when(scoreService.submitScore(any(), any(), any(Double.class), any(), any())).thenReturn(score);

        ResponseEntity<ScoreResponse> response = scoreController.submitScore(req);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Score submitted successfully", response.getBody().message());
        assertEquals(10f, response.getBody().score());
    }

    @Test
    void getScoreReturnsOk()
    {
        when(scoreService.getScore(score.getScoreId())).thenReturn(score);
        ResponseEntity<ScoreResponse> response = scoreController.getScore(score.getScoreId());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(score.getScoreId(), response.getBody().scoreId());
    }

    @Test
    void getScoreInfoDelegatesToService()
    {
        ScoreInfoResponse info = new ScoreInfoResponse(score.getScoreId(), 10f, "Cat",
                category.getCategoryId(), "u", "alice", user.getUserId(), false,
                new HashMap<>(), null, 1, 1L, 99.0, 1.5, 5f, 1f, 10);
        when(scoreService.getScoreInfo(score.getScoreId())).thenReturn(info);

        ResponseEntity<ScoreInfoResponse> response = scoreController.getScoreInfo(score.getScoreId());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(99.0, response.getBody().percentile());
    }

    @Test
    void getUserScoresReturnsList()
    {
        ScoreInfoResponse info = new ScoreInfoResponse(score.getScoreId(), 10f, "Cat",
                category.getCategoryId(), "u", "alice", user.getUserId(), false,
                new HashMap<>(), null, 1, 1L, 99.0, 1.5, 5f, 1f, 10);
        Page<ScoreInfoResponse> page = new PageImpl<>(List.of(info));
        when(scoreService.getUserBestScoreInfo(eq("alice"), anyInt(), anyInt())).thenReturn(page);

        ResponseEntity<List<ScoreInfoResponse>> response = scoreController.getUserScores("alice", 0, 25);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals(1, response.getBody().get(0).rank());
    }

}
