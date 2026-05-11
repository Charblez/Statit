/**
 * Filename: LeaderboardController.java
 * Author: Wilson Jimenez
 * Description: API controller for category leaderboards and baseline statistics retrieval.
 */

//----------------------------------------------------------------------------------------------------
// Package
//----------------------------------------------------------------------------------------------------
package com.statit.backend.controller;

//----------------------------------------------------------------------------------------------------
// Imports
//----------------------------------------------------------------------------------------------------
import com.statit.backend.dto.CorrelationResponse;
import com.statit.backend.dto.GlobalBaselineResponse;
import com.statit.backend.dto.LeaderboardResponse;
import com.statit.backend.dto.LeaderboardSnapshotResponse;
import com.statit.backend.dto.ScoreFilterRequest;
import com.statit.backend.model.Category;
import com.statit.backend.model.GlobalBaseline;
import com.statit.backend.model.Score;
import com.statit.backend.service.CategoryService;
import com.statit.backend.service.ScoreService;
import com.statit.backend.repository.GlobalBaselineRepository;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

//----------------------------------------------------------------------------------------------------
// Class Definition
//----------------------------------------------------------------------------------------------------
@RestController
@RequestMapping("/api/v1/leaderboards")
public class LeaderboardController
{
    //------------------------------------------------------------------------------------------------
    // Constructors
    //------------------------------------------------------------------------------------------------
    public LeaderboardController(ScoreService scoreService,
                                 CategoryService categoryService,
                                 GlobalBaselineRepository globalBaselineRepository)
    {
        this.scoreService = scoreService;
        this.categoryService = categoryService;
        this.globalBaselineRepository = globalBaselineRepository;
    }

    //------------------------------------------------------------------------------------------------
    // Public Methods
    //------------------------------------------------------------------------------------------------
    @GetMapping("/{categoryId}/top")
    public ResponseEntity<LeaderboardResponse> getTopScores(@PathVariable UUID categoryId,
                                                            @RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "25") int size)
    {
        Category category = categoryService.getLiveCategory(categoryId);
        Page<Score> scores = scoreService.getGlobalTopScores(categoryId, page, size);

        LeaderboardResponse response = LeaderboardResponse.fromPage(category, scores);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{categoryId}/filtered")
    public ResponseEntity<LeaderboardResponse> getFilteredTopScores(@PathVariable UUID categoryId,
                                                                    @RequestParam(defaultValue = "0") int page,
                                                                    @RequestParam(defaultValue = "25") int size,
                                                                    @RequestBody ScoreFilterRequest request)
    {
        Category category = categoryService.getLiveCategory(categoryId);
        Page<Score> scores = scoreService.getFilteredTopScores(categoryId, request.tags(), page, size);

        LeaderboardResponse response = LeaderboardResponse.fromPage(category, scores);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{categoryId}/snapshot")
    public ResponseEntity<LeaderboardSnapshotResponse> getLeaderboardSnapshot(@PathVariable UUID categoryId,
                                                                              @RequestParam(defaultValue = "0") int page,
                                                                              @RequestParam(defaultValue = "25") int size)
    {
        Category category = categoryService.getLiveCategory(categoryId);
        Page<Score> scores = scoreService.getGlobalTopScores(categoryId, page, size);
        LeaderboardResponse leaderboardResponse = LeaderboardResponse.fromPage(category, scores);

        List<GlobalBaseline> baselines = globalBaselineRepository.findAllByCategory(category);
        List<GlobalBaselineResponse> baselineResponses = new ArrayList<>();
        for(GlobalBaseline baseline : baselines)
        {
            baselineResponses.add(GlobalBaselineResponse.fromGlobalBaseline(baseline));
        }

        LeaderboardSnapshotResponse response = new LeaderboardSnapshotResponse(
                categoryId,
                leaderboardResponse,
                baselineResponses
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{categoryId}/baselines")
    public ResponseEntity<List<GlobalBaselineResponse>> getBaselineStats(@PathVariable UUID categoryId)
    {
        Category category = categoryService.getLiveCategory(categoryId);
        List<GlobalBaseline> baselines = globalBaselineRepository.findAllByCategory(category);
        List<GlobalBaselineResponse> responses = new ArrayList<>();

        for(GlobalBaseline baseline : baselines)
        {
            responses.add(GlobalBaselineResponse.fromGlobalBaseline(baseline));
        }

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{categoryId}/correlation")
    public ResponseEntity<CorrelationResponse> getCorrelation(@PathVariable UUID categoryId,
                                                              @RequestParam UUID otherCategoryId)
    {
        CorrelationResponse response = scoreService.getCorrelation(categoryId, otherCategoryId);
        return ResponseEntity.ok(response);
    }

    //------------------------------------------------------------------------------------------------
    // Private Variables
    //------------------------------------------------------------------------------------------------
    private final ScoreService scoreService;
    private final CategoryService categoryService;
    private final GlobalBaselineRepository globalBaselineRepository;
}
