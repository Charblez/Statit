package com.statit.backend.service;

import com.statit.backend.TestUtils;
import com.statit.backend.dto.ScoreInfoResponse;
import com.statit.backend.model.Category;
import com.statit.backend.model.GlobalBaseline;
import com.statit.backend.model.Score;
import com.statit.backend.model.User;
import com.statit.backend.repository.CategoryRepository;
import com.statit.backend.repository.GlobalBaselineRepository;
import com.statit.backend.repository.ScoreRepository;
import com.statit.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScoreServiceTest
{
    @Mock private ScoreRepository scoreRepository;
    @Mock private UserRepository userRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private GlobalBaselineRepository globalBaselineRepository;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks private ScoreService scoreService;

    private User user;
    private Category descCategory;
    private Category ascCategory;
    private UUID userId;
    private UUID categoryId;

    @BeforeEach
    void setUp()
    {
        userId = UUID.randomUUID();
        categoryId = UUID.randomUUID();

        user = new User("alice", "a@x", "h", LocalDate.now().minusYears(20), new HashMap<>());
        TestUtils.setField(user, "userId", userId);

        descCategory = new Category("Run", "d", "s", null, true, user);
        descCategory.setLive(true);
        TestUtils.setField(descCategory, "categoryId", categoryId);

        ascCategory = new Category("Time", "d", "s", null, false, user);
        ascCategory.setLive(true);
        TestUtils.setField(ascCategory, "categoryId", categoryId);
    }

    private Score makeScore(Category category, float scoreValue)
    {
        Score s = new Score(category, user, scoreValue, new HashMap<>(), false);
        TestUtils.setField(s, "scoreId", UUID.randomUUID());
        return s;
    }

    private GlobalBaseline emptyBaseline(Category category)
    {
        return new GlobalBaseline(category, new HashMap<>(), 0f, 0f, 0f, null, null, null, 0, "src");
    }

    // --------------------------------------------------------------------------------------------
    // submitScore
    // --------------------------------------------------------------------------------------------
    @Test
    void submitScoreRejectsNegativeValue()
    {
        assertThrows(IllegalArgumentException.class,
                () -> scoreService.submitScore(userId, categoryId, -1f, null, false));
    }

    @Test
    void submitScoreRejectsTooLargeValue()
    {
        assertThrows(IllegalArgumentException.class,
                () -> scoreService.submitScore(userId, categoryId, 1_000_000_000_000f, null, false));
    }

    @Test
    void submitScoreFirstScoreUpdatesBaseline()
    {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(descCategory));

        // No top score before; new top score after save
        when(scoreRepository.findFirstByCategoryAndUserOrderByScoreDesc(descCategory, user))
                .thenReturn(Optional.empty())
                .thenAnswer(inv -> {
                    Score s = makeScore(descCategory, 50f);
                    return Optional.of(s);
                });
        when(scoreRepository.save(any(Score.class))).thenAnswer(inv -> inv.getArgument(0));

        GlobalBaseline baseline = emptyBaseline(descCategory);
        when(globalBaselineRepository.findByCategory(descCategory)).thenReturn(Optional.of(baseline));

        Score result = scoreService.submitScore(userId, categoryId, 50f, null, false);

        assertEquals(50f, result.getScore());
        verify(scoreRepository).save(any(Score.class));
        verify(globalBaselineRepository).save(baseline);
        assertEquals(1, baseline.getSampleSize());
        assertEquals(50f, baseline.getMean());
        assertEquals(0f, baseline.getStandardDeviation());
    }

    @Test
    void submitScoreAddsTagsAndDemographicsAndAge()
    {
        user.getDemographics().put("region", "us");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(descCategory));
        when(scoreRepository.findFirstByCategoryAndUserOrderByScoreDesc(descCategory, user))
                .thenReturn(Optional.empty())
                .thenAnswer(inv -> Optional.of(makeScore(descCategory, 10f)));
        when(scoreRepository.save(any(Score.class))).thenAnswer(inv -> inv.getArgument(0));
        when(globalBaselineRepository.findByCategory(descCategory))
                .thenReturn(Optional.of(emptyBaseline(descCategory)));

        Map<String, String> userTags = new HashMap<>();
        userTags.put("session", "a");

        Score result = scoreService.submitScore(userId, categoryId, 10f, userTags, null);

        assertEquals("a", result.getTags().get("session"));
        assertEquals("us", result.getTags().get("region"));
        assertNotNull(result.getTags().get("age_months"));
        assertNotNull(result.getTags().get("age_years"));
        assertFalse(result.getAnonymous());
    }

    @Test
    void submitScoreRespectsAnonymousFlag()
    {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(descCategory));
        when(scoreRepository.findFirstByCategoryAndUserOrderByScoreDesc(descCategory, user))
                .thenReturn(Optional.empty())
                .thenAnswer(inv -> Optional.of(makeScore(descCategory, 1f)));
        when(scoreRepository.save(any(Score.class))).thenAnswer(inv -> inv.getArgument(0));
        when(globalBaselineRepository.findByCategory(descCategory))
                .thenReturn(Optional.of(emptyBaseline(descCategory)));

        Score result = scoreService.submitScore(userId, categoryId, 1f, null, true);
        assertTrue(result.getAnonymous());
    }

    @Test
    void submitScoreUserNotFoundThrows()
    {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> scoreService.submitScore(userId, categoryId, 5f, null, false));
    }

    @Test
    void submitScoreCategoryNotFoundThrows()
    {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> scoreService.submitScore(userId, categoryId, 5f, null, false));
    }

    @Test
    void submitScorePendingCategoryThrows()
    {
        descCategory.setLive(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(descCategory));

        assertThrows(IllegalArgumentException.class,
                () -> scoreService.submitScore(userId, categoryId, 5f, null, false));
        verify(scoreRepository, never()).save(any());
    }

    @Test
    void submitScoreReplaceExistingTopScoreUpdatesBaselineTwice()
    {
        Score oldTop = makeScore(descCategory, 10f);
        Score newTop = makeScore(descCategory, 20f);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(descCategory));
        when(scoreRepository.findFirstByCategoryAndUserOrderByScoreDesc(descCategory, user))
                .thenReturn(Optional.of(oldTop))
                .thenReturn(Optional.of(newTop));
        when(scoreRepository.save(any(Score.class))).thenAnswer(inv -> inv.getArgument(0));

        GlobalBaseline baseline = new GlobalBaseline(descCategory, new HashMap<>(), 10f, 0f, 0f,
                null, null, null, 1, "src");
        when(globalBaselineRepository.findByCategory(descCategory)).thenReturn(Optional.of(baseline));

        scoreService.submitScore(userId, categoryId, 20f, null, false);

        // remove 10 then add 20 -> sampleSize stays 1, mean = 20
        verify(globalBaselineRepository, atLeastOnce()).save(baseline);
        assertEquals(1, baseline.getSampleSize());
        assertEquals(20f, baseline.getMean());
    }

    // --------------------------------------------------------------------------------------------
    // getScore
    // --------------------------------------------------------------------------------------------
    @Test
    void getScoreReturnsScore()
    {
        Score s = makeScore(descCategory, 1f);
        when(scoreRepository.findById(s.getScoreId())).thenReturn(Optional.of(s));
        assertSame(s, scoreService.getScore(s.getScoreId()));
    }

    @Test
    void getScoreMissingThrows()
    {
        UUID id = UUID.randomUUID();
        when(scoreRepository.findById(id)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> scoreService.getScore(id));
    }

    // --------------------------------------------------------------------------------------------
    // deleteScore
    // --------------------------------------------------------------------------------------------
    @Test
    void deleteScoreNonTopDoesNotUpdateBaseline()
    {
        Score toDelete = makeScore(descCategory, 5f);
        Score topScore = makeScore(descCategory, 10f);

        when(scoreRepository.findById(toDelete.getScoreId())).thenReturn(Optional.of(toDelete));
        when(scoreRepository.findFirstByCategoryAndUserOrderByScoreDesc(descCategory, user))
                .thenReturn(Optional.of(topScore));

        scoreService.deleteScore(toDelete.getScoreId());

        verify(scoreRepository).delete(toDelete);
        verify(globalBaselineRepository, never()).save(any(GlobalBaseline.class));
    }

    @Test
    void deleteScoreTopWithFallbackUpdatesBaselineTwice()
    {
        Score topScore = makeScore(descCategory, 10f);
        Score fallback = makeScore(descCategory, 5f);

        when(scoreRepository.findById(topScore.getScoreId())).thenReturn(Optional.of(topScore));
        when(scoreRepository.findFirstByCategoryAndUserOrderByScoreDesc(descCategory, user))
                .thenReturn(Optional.of(topScore))
                .thenReturn(Optional.of(fallback));

        GlobalBaseline baseline = new GlobalBaseline(descCategory, new HashMap<>(), 10f, 0f, 0f,
                null, null, null, 1, "src");
        when(globalBaselineRepository.findByCategory(descCategory)).thenReturn(Optional.of(baseline));

        scoreService.deleteScore(topScore.getScoreId());

        verify(scoreRepository).delete(topScore);
        verify(globalBaselineRepository, atLeast(2)).save(baseline);
    }

    @Test
    void deleteScoreTopWithoutFallbackZeroesBaseline()
    {
        Score topScore = makeScore(descCategory, 10f);

        when(scoreRepository.findById(topScore.getScoreId())).thenReturn(Optional.of(topScore));
        when(scoreRepository.findFirstByCategoryAndUserOrderByScoreDesc(descCategory, user))
                .thenReturn(Optional.of(topScore))
                .thenReturn(Optional.empty());

        GlobalBaseline baseline = new GlobalBaseline(descCategory, new HashMap<>(), 10f, 0f, 0f,
                null, null, null, 1, "src");
        when(globalBaselineRepository.findByCategory(descCategory)).thenReturn(Optional.of(baseline));

        scoreService.deleteScore(topScore.getScoreId());

        assertEquals(0, baseline.getSampleSize());
        assertEquals(0f, baseline.getMean());
    }

    @Test
    void deleteScoreMissingThrows()
    {
        UUID id = UUID.randomUUID();
        when(scoreRepository.findById(id)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> scoreService.deleteScore(id));
    }

    // --------------------------------------------------------------------------------------------
    // getGlobalTopScores / getFilteredTopScores / getUserScores
    // --------------------------------------------------------------------------------------------
    @Test
    void getGlobalTopScoresDescUsesDescQuery()
    {
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(descCategory));
        Page<Score> page = new PageImpl<>(List.of());
        when(scoreRepository.findTopScoresPerUserDesc(eq(categoryId), any(Pageable.class))).thenReturn(page);

        Page<Score> result = scoreService.getGlobalTopScores(categoryId, 0, 5);
        assertSame(page, result);
        verify(scoreRepository, never()).findTopScoresPerUserAsc(any(), any());
    }

    @Test
    void getGlobalTopScoresAscUsesAscQuery()
    {
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(ascCategory));
        Page<Score> page = new PageImpl<>(List.of());
        when(scoreRepository.findTopScoresPerUserAsc(eq(categoryId), any(Pageable.class))).thenReturn(page);

        Page<Score> result = scoreService.getGlobalTopScores(categoryId, 0, 5);
        assertSame(page, result);
        verify(scoreRepository, never()).findTopScoresPerUserDesc(any(), any());
    }

    @Test
    void getGlobalTopScoresMissingCategoryThrows()
    {
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> scoreService.getGlobalTopScores(categoryId, 0, 5));
    }

    @Test
    void getFilteredTopScoresDescSerializesTags() throws Exception
    {
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(descCategory));
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"k\":\"v\"}");
        Page<Score> page = new PageImpl<>(List.of());
        when(scoreRepository.findFilteredTopScoresPerUserDesc(eq(categoryId), anyString(), any(Pageable.class)))
                .thenReturn(page);

        Map<String, String> tags = new HashMap<>();
        tags.put("k", "v");
        Page<Score> result = scoreService.getFilteredTopScores(categoryId, tags, 0, 5);
        assertSame(page, result);
    }

    @Test
    void getFilteredTopScoresAscWithEmptyTagsUsesEmptyJson()
    {
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(ascCategory));
        Page<Score> page = new PageImpl<>(List.of());
        when(scoreRepository.findFilteredTopScoresPerUserAsc(eq(categoryId), eq("{}"), any(Pageable.class)))
                .thenReturn(page);

        Page<Score> result = scoreService.getFilteredTopScores(categoryId, null, 0, 5);
        assertSame(page, result);
    }

    @Test
    void getUserScoresDelegatesToRepository()
    {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        Page<Score> page = new PageImpl<>(List.of());
        when(scoreRepository.findByUserOrderBySubmittedAtDesc(eq(user), any(Pageable.class))).thenReturn(page);

        Page<Score> result = scoreService.getUserScores("alice", 0, 25);
        assertSame(page, result);
    }

    @Test
    void getUserScoresUserNotFoundThrows()
    {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> scoreService.getUserScores("ghost", 0, 25));
    }

    // --------------------------------------------------------------------------------------------
    // getScoreInfo
    // --------------------------------------------------------------------------------------------
    @Test
    void getScoreInfoComputesPercentileWhenStdDevPositive()
    {
        Score score = makeScore(descCategory, 10f);

        when(scoreRepository.findById(score.getScoreId())).thenReturn(Optional.of(score));
        when(scoreRepository.findFirstByCategoryAndUserOrderByScoreDesc(descCategory, user))
                .thenReturn(Optional.of(score));
        when(scoreRepository.countUsersWithBetterScoreDesc(categoryId, 10.0)).thenReturn(2L);
        when(scoreRepository.countDistinctUsersByCategoryAndRejectedFalse(categoryId)).thenReturn(10L);

        GlobalBaseline baseline = new GlobalBaseline(descCategory, new HashMap<>(),
                5f, 0f, 2f, null, null, null, 5, "src");
        when(globalBaselineRepository.findByCategory(descCategory)).thenReturn(Optional.of(baseline));

        ScoreInfoResponse response = scoreService.getScoreInfo(score.getScoreId());

        assertEquals(3, response.rank());
        assertEquals(10L, response.totalParticipants());
        assertEquals("alice", response.username());
        assertNotNull(response.zScore());
        assertNotNull(response.percentile());
        assertEquals(2.5, response.zScore(), 1e-3);
        assertTrue(response.percentile() > 99.0);
    }

    @Test
    void getScoreInfoAscendingPercentileIsInverted()
    {
        Score score = makeScore(ascCategory, 3f);

        when(scoreRepository.findById(score.getScoreId())).thenReturn(Optional.of(score));
        when(scoreRepository.findFirstByCategoryAndUserOrderByScoreAsc(ascCategory, user))
                .thenReturn(Optional.of(score));
        when(scoreRepository.countUsersWithBetterScoreAsc(categoryId, 3.0)).thenReturn(0L);
        when(scoreRepository.countDistinctUsersByCategoryAndRejectedFalse(categoryId)).thenReturn(5L);

        GlobalBaseline baseline = new GlobalBaseline(ascCategory, new HashMap<>(),
                5f, 0f, 1f, null, null, null, 5, "src");
        when(globalBaselineRepository.findByCategory(ascCategory)).thenReturn(Optional.of(baseline));

        ScoreInfoResponse response = scoreService.getScoreInfo(score.getScoreId());

        assertEquals(1, response.rank());
        // Lower is better: percentile should be high
        assertTrue(response.percentile() > 90.0);
    }

    @Test
    void getScoreInfoNoBaselineLeavesPercentileNull()
    {
        Score score = makeScore(descCategory, 10f);

        when(scoreRepository.findById(score.getScoreId())).thenReturn(Optional.of(score));
        when(scoreRepository.findFirstByCategoryAndUserOrderByScoreDesc(descCategory, user))
                .thenReturn(Optional.of(score));
        when(scoreRepository.countUsersWithBetterScoreDesc(categoryId, 10.0)).thenReturn(0L);
        when(scoreRepository.countDistinctUsersByCategoryAndRejectedFalse(categoryId)).thenReturn(1L);
        when(globalBaselineRepository.findByCategory(descCategory)).thenReturn(Optional.empty());

        ScoreInfoResponse response = scoreService.getScoreInfo(score.getScoreId());

        assertNull(response.percentile());
        assertNull(response.zScore());
        assertNull(response.baselineMean());
    }

    @Test
    void getScoreInfoZeroStdDevEqualsMeanReturns50()
    {
        Score score = makeScore(descCategory, 5f);

        when(scoreRepository.findById(score.getScoreId())).thenReturn(Optional.of(score));
        when(scoreRepository.findFirstByCategoryAndUserOrderByScoreDesc(descCategory, user))
                .thenReturn(Optional.of(score));
        when(scoreRepository.countUsersWithBetterScoreDesc(categoryId, 5.0)).thenReturn(0L);
        when(scoreRepository.countDistinctUsersByCategoryAndRejectedFalse(categoryId)).thenReturn(1L);

        GlobalBaseline baseline = new GlobalBaseline(descCategory, new HashMap<>(),
                5f, 0f, 0f, null, null, null, 1, "src");
        when(globalBaselineRepository.findByCategory(descCategory)).thenReturn(Optional.of(baseline));

        ScoreInfoResponse response = scoreService.getScoreInfo(score.getScoreId());
        assertEquals(50.0, response.percentile());
    }

    @Test
    void getScoreInfoZeroStdDevDescAboveMeanReturns100()
    {
        Score score = makeScore(descCategory, 10f);

        when(scoreRepository.findById(score.getScoreId())).thenReturn(Optional.of(score));
        when(scoreRepository.findFirstByCategoryAndUserOrderByScoreDesc(descCategory, user))
                .thenReturn(Optional.of(score));
        when(scoreRepository.countUsersWithBetterScoreDesc(categoryId, 10.0)).thenReturn(0L);
        when(scoreRepository.countDistinctUsersByCategoryAndRejectedFalse(categoryId)).thenReturn(1L);

        GlobalBaseline baseline = new GlobalBaseline(descCategory, new HashMap<>(),
                5f, 0f, 0f, null, null, null, 1, "src");
        when(globalBaselineRepository.findByCategory(descCategory)).thenReturn(Optional.of(baseline));

        ScoreInfoResponse response = scoreService.getScoreInfo(score.getScoreId());
        assertEquals(100.0, response.percentile());
    }

    @Test
    void getScoreInfoAnonymousMasksUsername()
    {
        Score score = new Score(descCategory, user, 10f, new HashMap<>(), true);
        TestUtils.setField(score, "scoreId", UUID.randomUUID());

        when(scoreRepository.findById(score.getScoreId())).thenReturn(Optional.of(score));
        when(scoreRepository.findFirstByCategoryAndUserOrderByScoreDesc(descCategory, user))
                .thenReturn(Optional.of(score));
        when(scoreRepository.countUsersWithBetterScoreDesc(categoryId, 10.0)).thenReturn(0L);
        when(scoreRepository.countDistinctUsersByCategoryAndRejectedFalse(categoryId)).thenReturn(1L);
        when(globalBaselineRepository.findByCategory(descCategory)).thenReturn(Optional.empty());

        ScoreInfoResponse response = scoreService.getScoreInfo(score.getScoreId());
        assertEquals("Anonymous", response.username());
        assertNull(response.userId());
    }

    @Test
    void getScoreInfoMissingThrows()
    {
        UUID id = UUID.randomUUID();
        when(scoreRepository.findById(id)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> scoreService.getScoreInfo(id));
    }
}
