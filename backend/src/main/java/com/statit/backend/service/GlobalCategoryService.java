package com.statit.backend.service;

import com.statit.backend.dto.GlobalDatasetResponse;
import com.statit.backend.dto.GlobalStatCompareResponse;
import com.statit.backend.model.Category;
import com.statit.backend.model.CategoryScope;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class GlobalCategoryService
{
    public GlobalCategoryService(CategoryService categoryService,
                                 List<GlobalDatasetProvider> providers)
    {
        this.categoryService = categoryService;
        this.providers = new HashMap<>();
        for(GlobalDatasetProvider provider : providers)
        {
            this.providers.put(provider.sourceKey(), provider);
        }
    }

    public Page<Category> getGlobalCategories(Pageable pageable)
    {
        return categoryService.getGlobalCategories(pageable);
    }

    public GlobalDatasetResponse getDataset(UUID categoryId, Map<String, String> tags)
    {
        Category category = getGlobalCategory(categoryId);
        GlobalDatasetSnapshot snapshot = getProvider(category).getSnapshot(tags);
        return new GlobalDatasetResponse(
                category.getCategoryId(),
                category.getName(),
                category.getUnits(),
                snapshot.sourceName(),
                snapshot.sourceUrl(),
                snapshot.mean(),
                snapshot.standardDeviation(),
                snapshot.sampleSize(),
                snapshot.values(),
                snapshot.histogram(),
                snapshot.scatterPoints()
        );
    }

    public GlobalStatCompareResponse compare(UUID categoryId, Double score, Map<String, String> tags)
    {
        if(score == null)
        {
            throw new IllegalArgumentException("Score is required.");
        }

        Category category = getGlobalCategory(categoryId);
        if(category.getLowerLimit() != null && score < category.getLowerLimit())
        {
            throw new IllegalArgumentException("Score must be at least " + category.getLowerLimit() + " for this category.");
        }
        if(category.getUpperLimit() != null && score > category.getUpperLimit())
        {
            throw new IllegalArgumentException("Score cannot exceed " + category.getUpperLimit() + " for this category.");
        }

        GlobalDatasetSnapshot snapshot = getProvider(category).getSnapshot(tags);
        double zScore = snapshot.standardDeviation() > 0
                ? (score - snapshot.mean()) / snapshot.standardDeviation()
                : 0.0;
        double percentile = calculatePercentile(score, snapshot, zScore);
        int rankEstimate = estimateRank(category, percentile, snapshot.sampleSize());

        return new GlobalStatCompareResponse(
                category.getCategoryId(),
                category.getName(),
                category.getUnits(),
                score,
                clamp(percentile, 0.0, 100.0),
                rankEstimate,
                zScore,
                snapshot.mean(),
                snapshot.standardDeviation(),
                snapshot.sampleSize(),
                snapshot.sourceName(),
                snapshot.sourceUrl(),
                snapshot.histogram(),
                snapshot.scatterPoints(),
                "Global category comparison calculated without saving a score."
        );
    }

    private Category getGlobalCategory(UUID categoryId)
    {
        Category category = categoryService.getLiveCategory(categoryId);
        if(category.getCategoryScope() != CategoryScope.GLOBAL)
        {
            throw new IllegalArgumentException("Category is not global.");
        }
        return category;
    }

    private GlobalDatasetProvider getProvider(Category category)
    {
        GlobalDatasetProvider provider = providers.get(category.getGlobalSourceKey());
        if(provider == null)
        {
            throw new IllegalArgumentException("No global dataset provider is configured for this category.");
        }
        return provider;
    }

    private int estimateRank(Category category, double percentile, int sampleSize)
    {
        double above = sampleSize * ((100.0 - percentile) / 100.0);
        double below = sampleSize * (percentile / 100.0);
        int rank = category.getSortOrder() ? (int) Math.round(above) + 1 : (int) Math.round(below) + 1;
        return Math.max(rank, 1);
    }

    private double calculatePercentile(Double score, GlobalDatasetSnapshot snapshot, double zScore)
    {
        if(snapshot.values() != null && !snapshot.values().isEmpty())
        {
            long lessOrEqual = snapshot.values().stream()
                    .filter(value -> value != null && value <= score)
                    .count();
            return ((double) lessOrEqual / snapshot.values().size()) * 100.0;
        }

        if(snapshot.histogram() != null && !snapshot.histogram().isEmpty())
        {
            double total = snapshot.histogram().stream()
                    .mapToDouble(bin -> bin.count() != null ? bin.count() : 0.0)
                    .sum();
            if(total <= 0) return 50.0;

            double atOrBelow = 0.0;
            for(var bin : snapshot.histogram())
            {
                if(bin.count() == null || bin.start() == null || bin.end() == null) continue;

                if(score >= bin.end())
                {
                    atOrBelow += bin.count();
                }
                else if(score > bin.start())
                {
                    double ratio = (score - bin.start()) / (bin.end() - bin.start());
                    atOrBelow += bin.count() * Math.min(Math.max(ratio, 0.0), 1.0);
                    break;
                }
                else
                {
                    break;
                }
            }
            return (atOrBelow / total) * 100.0;
        }

        return snapshot.standardDeviation() > 0 ? normalCdf(zScore) * 100.0 : 50.0;
    }

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

    private double clamp(double value, double min, double max)
    {
        return Math.min(Math.max(value, min), max);
    }

    private final CategoryService categoryService;
    private final Map<String, GlobalDatasetProvider> providers;
}
