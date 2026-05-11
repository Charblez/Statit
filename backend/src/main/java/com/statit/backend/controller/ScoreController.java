/**
 * Filename: ScoreController.java
 * Author: Wilson Jimenez
 * Description: API controller for score submission, retrieval, and deletion.
 */

//----------------------------------------------------------------------------------------------------
// Package
//----------------------------------------------------------------------------------------------------
package com.statit.backend.controller;

//----------------------------------------------------------------------------------------------------
// Imports
//----------------------------------------------------------------------------------------------------
import com.statit.backend.dto.ScoreInfoResponse;
import com.statit.backend.dto.ScoreResponse;
import com.statit.backend.dto.ScoreSubmitRequest;
import com.statit.backend.model.Score;
import com.statit.backend.service.ScoreService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

//----------------------------------------------------------------------------------------------------
// Class Definition
//----------------------------------------------------------------------------------------------------
@RestController
@RequestMapping("/api/v1/scores")
public class ScoreController
{
    //------------------------------------------------------------------------------------------------
    // Constructors
    //------------------------------------------------------------------------------------------------
    public ScoreController(ScoreService scoreService)
    {
        this.scoreService = scoreService;
    }

    //------------------------------------------------------------------------------------------------
    // Public Methods
    //------------------------------------------------------------------------------------------------
    @PostMapping
    public ResponseEntity<ScoreResponse> submitScore(@RequestBody ScoreSubmitRequest request)
    {
        Score newScore = scoreService.submitScore(
                request.userId(),
                request.categoryId(),
                request.score(),
                request.tags(),
                request.anonymous()
        );

        ScoreResponse response = ScoreResponse.fromScore(newScore, "Score submitted successfully");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{scoreId}")
    public ResponseEntity<ScoreResponse> getScore(@PathVariable UUID scoreId)
    {
        Score score = scoreService.getScore(scoreId);
        ScoreResponse response = ScoreResponse.fromScore(score, null);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{scoreId}/info")
    public ResponseEntity<ScoreInfoResponse> getScoreInfo(@PathVariable UUID scoreId)
    {
        ScoreInfoResponse response = scoreService.getScoreInfo(scoreId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{username}")
    public ResponseEntity<List<ScoreInfoResponse>> getUserScores(@PathVariable String username,
                                                                 @RequestParam(defaultValue = "0") int page,
                                                                 @RequestParam(defaultValue = "25") int size)
    {
        Page<ScoreInfoResponse> scores = scoreService.getUserBestScoreInfo(username, page, size);
        return ResponseEntity.ok(scores.getContent());
    }

    @GetMapping("/user/{username}/category/{categoryId}/top")
    public ResponseEntity<ScoreInfoResponse> getUserTopScoreForCategory(@PathVariable String username,
                                                                        @PathVariable UUID categoryId)
    {
        ScoreInfoResponse response = scoreService.getUserTopScoreInfoForCategory(username, categoryId);
        return ResponseEntity.ok(response);
    }

    //------------------------------------------------------------------------------------------------
    // Private Variables
    //------------------------------------------------------------------------------------------------
    private final ScoreService scoreService;
}
