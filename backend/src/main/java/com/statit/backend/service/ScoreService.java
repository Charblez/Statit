/**
 * Filename: ScoreService.java
 * Author: Charles Bassani
 * Description: Handles CRUD operations for scores, updates global baselines, and computes ranking statistics.
 */

//----------------------------------------------------------------------------------------------------
// Package
//----------------------------------------------------------------------------------------------------
package com.statit.backend.service;

//----------------------------------------------------------------------------------------------------
// Imports
//----------------------------------------------------------------------------------------------------
import com.statit.backend.dto.ScoreInfoResponse;
import com.statit.backend.model.Category;
import com.statit.backend.model.GlobalBaseline;
import com.statit.backend.model.Score;
import com.statit.backend.model.User;
import com.statit.backend.repository.CategoryRepository;
import com.statit.backend.repository.GlobalBaselineRepository;
import com.statit.backend.repository.ScoreRepository;
import com.statit.backend.repository.UserRepository;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

//----------------------------------------------------------------------------------------------------
// Class Definition
//----------------------------------------------------------------------------------------------------
@Service
public class ScoreService
{
    //------------------------------------------------------------------------------------------------
    // Constructors
    //------------------------------------------------------------------------------------------------
    public ScoreService(ScoreRepository scoreRepository,
                        UserRepository userRepository,
                        CategoryRepository categoryRepository,
                        GlobalBaselineRepository globalBaselineRepository,
                        ObjectMapper objectMapper)
    {
        this.scoreRepository = scoreRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.globalBaselineRepository = globalBaselineRepository;
        this.objectMapper = objectMapper;
    }

    //------------------------------------------------------------------------------------------------
    // Public Methods
    //------------------------------------------------------------------------------------------------
    @Transactional
    public Score submitScore(UUID userId, UUID categoryId, Float scoreValue, Map<String, String> scoreTags, Boolean isAnonymous)
    {
        return submitScore(
                userId,
                categoryId,
                scoreValue != null ? scoreValue.doubleValue() : null,
                scoreTags,
                isAnonymous
        );
    }

    @Transactional
    public Score submitScore(UUID userId, UUID categoryId, Double scoreValue, Map<String, String> scoreTags, Boolean isAnonymous)
    {
        // Fetch the user and category
        if (scoreValue != null && (scoreValue < -99999999999D || scoreValue > 999999999999D)) {
    throw new IllegalArgumentException("Score must be between -999,999,999,999 and 999,999,999,999.");
}

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found."));
        requireLiveCategory(category);

        // ENFORCE THE DYNAMIC CATEGORY LIMITS HERE
        if (category.getLowerLimit() != null && scoreValue < category.getLowerLimit()) {
            throw new IllegalArgumentException("Score must be at least " + category.getLowerLimit() + " for this category.");
        }
        if (category.getUpperLimit() != null && scoreValue > category.getUpperLimit()) {
            throw new IllegalArgumentException("Score cannot exceed " + category.getUpperLimit() + " for this category.");
        }

        Score previousTopScore = getTopScoreForUser(category, user).orElse(null);

        // JSONB tags merge
        Map<String, String> finalTags = new HashMap<>();
        if(scoreTags != null) finalTags.putAll(scoreTags);
        if(user.getDemographics() != null) finalTags.putAll(user.getDemographics());
        finalTags.put("age_months", String.valueOf(user.getAgeMonths()));
        finalTags.put("age_years", String.valueOf(user.getAgeYears()));

        // Save the new score
        Score newScore = new Score(category, user, scoreValue, finalTags, Boolean.TRUE.equals(isAnonymous));
        scoreRepository.save(newScore);
        scoreRepository.flush();

        // Find new top score after saving
        Score newTopScore = getTopScoreForUser(category, user).orElse(null);

        // Update the global baseline
        if(previousTopScore == null)
        {
            updateGlobalBaseline(category, newTopScore.getScore(), false);
        }
        else if(!previousTopScore.getScoreId().equals(newTopScore.getScoreId()))
        {
            updateGlobalBaseline(category, previousTopScore.getScore(), true);
            updateGlobalBaseline(category, newTopScore.getScore(), false);
        }

        return newScore;
    }

    public Score getScore(UUID scoreId)
    {
        return scoreRepository.findById(scoreId)
                .orElseThrow(() -> new IllegalArgumentException("Score not found."));
    }

    @Transactional
    public void deleteScore(UUID scoreId)
    {
        Score scoreToDelete = scoreRepository.findById(scoreId)
                .orElseThrow(() -> new IllegalArgumentException("Score not found."));

        Category category = scoreToDelete.getCategory();
        User user = scoreToDelete.getUser();

        //Find users top score before deleting
        Score currentTopScore = getTopScoreForUser(category, user).orElse(null);

        //Determine if deleted score was top score
        boolean wasTopScore = currentTopScore != null && currentTopScore.getScoreId().equals(scoreToDelete.getScoreId());

        //Remove the score
        scoreRepository.delete(scoreToDelete);
        scoreRepository.flush();

        //Update the global baseline
        if(wasTopScore)
        {
            updateGlobalBaseline(category, scoreToDelete.getScore(), true);

            Score fallbackTopScore = getTopScoreForUser(category, user).orElse(null);
            if(fallbackTopScore != null)
            {
                updateGlobalBaseline(category, fallbackTopScore.getScore(), false);
            }
        }
    }

    public Page<Score> getGlobalTopScores(UUID categoryId, int page, int size)
    {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found."));
        requireLiveCategory(category);

        Pageable pageable = PageRequest.of(page, size);

        if(category.getSortOrder())
        {
            return scoreRepository.findTopScoresPerUserDesc(categoryId, pageable);
        }
        else
        {
            return scoreRepository.findTopScoresPerUserAsc(categoryId, pageable);
        }
    }

    public Page<Score> getFilteredTopScores(UUID categoryId, Map<String, String> tags, int page, int size)
    {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found."));
        requireLiveCategory(category);

        String tagsJson = serializeTagsToJson(tags);
        Pageable pageable = PageRequest.of(page, size);

        if(category.getSortOrder())
        {
            return scoreRepository.findFilteredTopScoresPerUserDesc(categoryId, tagsJson, pageable);
        }
        else
        {
            return scoreRepository.findFilteredTopScoresPerUserAsc(categoryId, tagsJson, pageable);
        }
    }

    public Page<Score> getUserScores(String username, int page, int size)
    {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        Pageable pageable = PageRequest.of(page, size);
        return scoreRepository.findByUserOrderBySubmittedAtDesc(user, pageable);
    }

    public ScoreInfoResponse getScoreInfo(UUID scoreId)
    {
        Score score = scoreRepository.findById(scoreId)
                .orElseThrow(() -> new IllegalArgumentException("Score not found."));

        Category category = score.getCategory();
        User user = score.getUser();

        //Find the user's top score in this category to determine rank
        Score topScore = getTopScoreForUser(category, user).orElse(score);

        //Get rank by counting how many users have a better top score
        int rank = computeRank(category, topScore.getScore());
        long totalParticipants = scoreRepository.countDistinctUsersByCategoryAndRejectedFalse(category.getCategoryId());

        //Get baseline stats for percentile calculation
        GlobalBaseline baseline = globalBaselineRepository.findByCategory(category).orElse(null);

        Double percentile = null;
        Double zScore = null;
        Float baselineMean = null;
        Float baselineStdDev = null;
        Integer baselineSampleSize = null;

        if(baseline != null)
        {
            baselineMean = baseline.getMean();
            baselineStdDev = baseline.getStandardDeviation();
            baselineSampleSize = baseline.getSampleSize();

            if(baselineMean != null && baselineStdDev != null && baselineStdDev > 0)
            {
                zScore = (score.getScore().doubleValue() - baselineMean.doubleValue()) / baselineStdDev.doubleValue();
                double cdf = normalCdf(zScore);

                //For descending categories (higher is better), percentile = CDF * 100
                //For ascending categories (lower is better), percentile = (1 - CDF) * 100
                if(category.getSortOrder())
                {
                    percentile = cdf * 100.0;
                }
                else
                {
                    percentile = (1.0 - cdf) * 100.0;
                }
            }
            else if(baselineSampleSize != null && baselineSampleSize > 0)
            {
                //If stddev is 0, everyone has the same score
                if(baselineMean != null && Double.compare(score.getScore(), baselineMean.doubleValue()) == 0)
                {
                    percentile = 50.0;
                }
                else if(baselineMean != null && category.getSortOrder())
                {
                    percentile = score.getScore() > baselineMean ? 100.0 : 0.0;
                }
                else if(baselineMean != null)
                {
                    percentile = score.getScore() < baselineMean ? 100.0 : 0.0;
                }
            }
        }

        return new ScoreInfoResponse(
                score.getScoreId(),
                score.getScore(),
                category.getName(),
                category.getCategoryId(),
                category.getUnits(),
                score.getAnonymous() ? "Anonymous" : user.getUsername(),
                score.getAnonymous() ? null : user.getUserId(),
                score.getAnonymous(),
                score.getTags(),
                score.getSubmittedAt(),
                rank,
                totalParticipants,
                percentile,
                zScore,
                baselineMean,
                baselineStdDev,
                baselineSampleSize
        );
    }

    //------------------------------------------------------------------------------------------------
    // Private Methods
    //------------------------------------------------------------------------------------------------
    private int computeRank(Category category, Double scoreValue)
    {
        if(category.getSortOrder())
        {
            //Descending: count users with a better (higher) top score + 1
            long betterCount = scoreRepository.countUsersWithBetterScoreDesc(category.getCategoryId(), scoreValue);
            return (int) betterCount + 1;
        }
        else
        {
            //Ascending: count users with a better (lower) top score + 1
            long betterCount = scoreRepository.countUsersWithBetterScoreAsc(category.getCategoryId(), scoreValue);
            return (int) betterCount + 1;
        }
    }

    private void requireLiveCategory(Category category)
    {
        if(!category.getLive())
        {
            throw new IllegalArgumentException("Category is pending admin approval.");
        }
    }

    private void updateGlobalBaseline(Category category, Double scoreD, Boolean removal)
    {
        //Fetch existing baseline
        GlobalBaseline baseline = globalBaselineRepository.findByCategory(category)
                .orElseThrow(() -> new IllegalArgumentException("Category not found."));

        Integer oldN = baseline.getSampleSize();
        if(oldN == null) oldN = 0;

        //Fail if removing and no entries
        if(oldN == 0 && removal) return;

        float score = scoreD == null ? 0.0f : scoreD.floatValue();

        Float oldMean = baseline.getMean();
        if(oldMean == null) oldMean = 0.0f;

        Float oldStdDev = baseline.getStandardDeviation();
        if(oldStdDev == null) oldStdDev = 0.0f;

        //Generate new values
        int newN = removal ? oldN - 1 : oldN + 1;
        float newMean;
        float newStdDev;

        if(removal)
        {
            if(newN == 0)
            {
                newMean = 0.0f;
                newStdDev = 0.0f;
            }
            else if(newN == 1)
            {
                newMean = (oldMean * oldN - score) / newN;
                newStdDev = 0.0f;
            }
            else
            {
                newMean = ((oldMean * oldN) - score) / newN;
                float oldVariance = oldStdDev * oldStdDev;
                float oldM2 = oldVariance * (oldN - 1);
                float newM2 = oldM2 - ((score - oldMean) * (score - newMean));
                newM2 = Math.max(0.0f, newM2);
                float newVariance = newM2 / (newN - 1);
                newStdDev = (float) Math.sqrt(newVariance);
            }
        }
        else
        {
            if(newN == 1)
            {
                newMean = score;
                newStdDev = 0.0f;
            }
            else
            {
                newMean = oldMean + ((score - oldMean) / newN);
                float oldVariance = oldStdDev * oldStdDev;
                float oldM2 = oldVariance * (oldN - 1);
                float newM2 = oldM2 + ((score - oldMean) * (score - newMean));
                float newVariance = newM2 / (newN - 1);
                newStdDev = (float) Math.sqrt(newVariance);
            }
        }

        //Update baseline record
        baseline.setMean(newMean);
        if(newN == 1)
        {
            baseline.setMedian(newMean);
        }
        else if(newN == 0)
        {
            baseline.setMedian(0.0f);
        }
        baseline.setStandardDeviation(newStdDev);
        baseline.setSampleSize(newN);

        globalBaselineRepository.save(baseline);
    }

    private Optional<Score> getTopScoreForUser(Category category, User user)
    {
        if(category.getSortOrder())
        {
            return scoreRepository.findFirstByCategoryAndUserOrderByScoreDesc(category, user);
        }
        else
        {
            return scoreRepository.findFirstByCategoryAndUserOrderByScoreAsc(category, user);
        }
    }

    private String serializeTagsToJson(Map<String, String> tags)
    {
        if(tags == null || tags.isEmpty()) return "{}";
        try
        {
            return objectMapper.writeValueAsString(tags);
        }
        catch(JacksonException e)
        {
            throw new IllegalArgumentException("Failed to serialize tags to JSON.", e);
        }
    }

    /**
     * Approximates the cumulative distribution function (CDF) of the standard normal distribution.
     * Uses the Abramowitz and Stegun error function approximation.
     */
    private double normalCdf(double z)
    {
        return 0.5 * (1.0 + erf(z / Math.sqrt(2.0)));
    }

    private double erf(double value)
    {
        double sign = value < 0 ? -1.0 : 1.0;
        double x = Math.abs(value);

        double a1 = 0.254829592;
        double a2 = -0.284496736;
        double a3 = 1.421413741;
        double a4 = -1.453152027;
        double a5 = 1.061405429;
        double p = 0.3275911;

        double t = 1.0 / (1.0 + p * x);
        double y = 1.0 - (((((a5 * t + a4) * t) + a3) * t + a2) * t + a1) * t * Math.exp(-x * x);
        return sign * y;
    }

    //------------------------------------------------------------------------------------------------
    // Private Variables
    //------------------------------------------------------------------------------------------------
    private final ScoreRepository scoreRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final GlobalBaselineRepository globalBaselineRepository;
    private final ObjectMapper objectMapper;
}
