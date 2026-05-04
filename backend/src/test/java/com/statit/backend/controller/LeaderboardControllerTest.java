package com.statit.backend.controller;

import com.statit.backend.TestUtils;
import com.statit.backend.dto.GlobalBaselineResponse;
import com.statit.backend.dto.LeaderboardResponse;
import com.statit.backend.dto.LeaderboardSnapshotResponse;
import com.statit.backend.dto.ScoreFilterRequest;
import com.statit.backend.model.Category;
import com.statit.backend.model.GlobalBaseline;
import com.statit.backend.model.Score;
import com.statit.backend.model.User;
import com.statit.backend.repository.GlobalBaselineRepository;
import com.statit.backend.service.CategoryService;
import com.statit.backend.service.ScoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaderboardControllerTest
{
    @Mock private ScoreService scoreService;
    @Mock private CategoryService categoryService;
    @Mock private GlobalBaselineRepository globalBaselineRepository;

    @InjectMocks private LeaderboardController leaderboardController;

    private User user;
    private Category category;
    private UUID categoryId;

    @BeforeEach
    void setUp()
    {
        user = new User("alice", "a@x", "h", LocalDate.of(2000, 1, 1), null);
        TestUtils.setField(user, "userId", UUID.randomUUID());
        category = new Category("Cat", "d", "u", null, true, user);
        category.setLive(true);
        categoryId = UUID.randomUUID();
        TestUtils.setField(category, "categoryId", categoryId);
    }

    private Score makeScore(float value)
    {
        Score s = new Score(category, user, value, new HashMap<>(), false);
        TestUtils.setField(s, "scoreId", UUID.randomUUID());
        return s;
    }

    @Test
    void getTopScoresReturnsLeaderboard()
    {
        Page<Score> page = new PageImpl<>(List.of(makeScore(10f)), PageRequest.of(0, 25), 1);
        when(categoryService.getLiveCategory(categoryId)).thenReturn(category);
        when(scoreService.getGlobalTopScores(eq(categoryId), anyInt(), anyInt())).thenReturn(page);

        ResponseEntity<LeaderboardResponse> response = leaderboardController.getTopScores(categoryId, 0, 25);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().scores().size());
        assertEquals("Cat", response.getBody().categoryName());
    }

    @Test
    void getFilteredTopScoresReturnsLeaderboard()
    {
        Page<Score> page = new PageImpl<>(List.of(makeScore(5f)), PageRequest.of(0, 10), 1);
        when(categoryService.getLiveCategory(categoryId)).thenReturn(category);
        when(scoreService.getFilteredTopScores(eq(categoryId), any(), anyInt(), anyInt())).thenReturn(page);

        HashMap<String, String> tags = new HashMap<>();
        tags.put("region", "us");
        ScoreFilterRequest req = new ScoreFilterRequest(tags);

        ResponseEntity<LeaderboardResponse> response = leaderboardController.getFilteredTopScores(
                categoryId, 0, 10, req);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().scores().size());
    }

    @Test
    void getLeaderboardSnapshotIncludesBaselines()
    {
        Page<Score> page = new PageImpl<>(List.of(makeScore(10f)), PageRequest.of(0, 25), 1);
        GlobalBaseline baseline = new GlobalBaseline(category, new HashMap<>(),
                5f, 0f, 1f, null, null, null, 5, "src");
        TestUtils.setField(baseline, "baselineId", UUID.randomUUID());

        when(categoryService.getLiveCategory(categoryId)).thenReturn(category);
        when(scoreService.getGlobalTopScores(eq(categoryId), anyInt(), anyInt())).thenReturn(page);
        when(globalBaselineRepository.findAllByCategory(category)).thenReturn(Arrays.asList(baseline));

        ResponseEntity<LeaderboardSnapshotResponse> response =
                leaderboardController.getLeaderboardSnapshot(categoryId, 0, 25);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(categoryId, response.getBody().categoryId());
        assertEquals(1, response.getBody().baselines().size());
        assertEquals(5f, response.getBody().baselines().get(0).mean());
    }

    @Test
    void getBaselineStatsReturnsAllBaselines()
    {
        GlobalBaseline baseline = new GlobalBaseline(category, new HashMap<>(),
                5f, 0f, 1f, null, null, null, 5, "src");
        TestUtils.setField(baseline, "baselineId", UUID.randomUUID());

        when(categoryService.getLiveCategory(categoryId)).thenReturn(category);
        when(globalBaselineRepository.findAllByCategory(category)).thenReturn(Arrays.asList(baseline));

        ResponseEntity<List<GlobalBaselineResponse>> response =
                leaderboardController.getBaselineStats(categoryId);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }
}
