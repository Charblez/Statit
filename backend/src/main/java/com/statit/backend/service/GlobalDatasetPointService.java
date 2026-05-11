package com.statit.backend.service;

import com.statit.backend.dto.GlobalHistogramBinResponse;
import com.statit.backend.model.Category;
import com.statit.backend.model.GlobalDatasetPoint;
import com.statit.backend.repository.CategoryRepository;
import com.statit.backend.repository.GlobalDatasetPointRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class GlobalDatasetPointService
{
    public GlobalDatasetPointService(CategoryRepository categoryRepository,
                                     GlobalDatasetPointRepository globalDatasetPointRepository)
    {
        this.categoryRepository = categoryRepository;
        this.globalDatasetPointRepository = globalDatasetPointRepository;
    }

    public GlobalDatasetSnapshot buildSnapshot(String sourceKey,
                                               String sourceName,
                                               String sourceUrl,
                                               Map<String, String> filters)
    {
        Category category = categoryRepository.findByGlobalSourceKey(sourceKey)
                .orElseThrow(() -> new IllegalArgumentException("Global category is not configured."));

        Map<String, String> normalizedFilters = normalizeFilters(filters);
        List<GlobalDatasetPoint> points = globalDatasetPointRepository.findAllByCategoryOrderByValueAsc(category)
                .stream()
                .filter(point -> matchesFilters(point, normalizedFilters))
                .toList();
        List<Double> values = points.stream()
                .map(GlobalDatasetPoint::getValue)
                .filter(value -> value != null && Double.isFinite(value))
                .toList();

        double mean = average(values);
        double stdDev = standardDeviation(values, mean);

        return new GlobalDatasetSnapshot(
                sourceName,
                sourceUrl,
                mean,
                stdDev,
                values.size(),
                values,
                buildHistogram(values),
                List.of()
        );
    }

    private Map<String, String> normalizeFilters(Map<String, String> filters)
    {
        Map<String, String> normalized = new LinkedHashMap<>();
        if(filters == null) return normalized;

        for(String allowedKey : ALLOWED_FILTER_KEYS)
        {
            String value = filters.get(allowedKey);
            if(value != null && !value.isBlank() && !"All".equalsIgnoreCase(value))
            {
                normalized.put(allowedKey, value.trim());
            }
        }
        return normalized;
    }

    private boolean matchesFilters(GlobalDatasetPoint point, Map<String, String> filters)
    {
        if(filters == null || filters.isEmpty()) return true;
        Map<String, String> demographics = point.getDemographics();
        if(demographics == null || demographics.isEmpty()) return false;

        for(Map.Entry<String, String> filter : filters.entrySet())
        {
            String storedValue = demographics.get(filter.getKey());
            if(!sameFilterValue(storedValue, filter.getValue())) return false;
        }
        return true;
    }

    private boolean sameFilterValue(String storedValue, String requestedValue)
    {
        if(storedValue == null || requestedValue == null) return false;
        return normalizeValue(storedValue).equals(normalizeValue(requestedValue));
    }

    private String normalizeValue(String value)
    {
        return value.trim().toLowerCase(Locale.US);
    }

    private double average(List<Double> values)
    {
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    private double standardDeviation(List<Double> values, double mean)
    {
        if(values.size() < 2) return 0.0;
        double variance = values.stream()
                .mapToDouble(value -> Math.pow(value - mean, 2))
                .sum() / (values.size() - 1);
        return Math.sqrt(variance);
    }

    private List<GlobalHistogramBinResponse> buildHistogram(List<Double> values)
    {
        List<GlobalHistogramBinResponse> bins = new ArrayList<>();
        if(values.isEmpty()) return bins;

        double min = values.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
        double max = values.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
        if(Double.compare(min, max) == 0)
        {
            bins.add(new GlobalHistogramBinResponse(round(min), round(max), (long)values.size()));
            return bins;
        }

        double binSize = (max - min) / HISTOGRAM_BIN_COUNT;
        for(int i = 0; i < HISTOGRAM_BIN_COUNT; i++)
        {
            double start = min + (i * binSize);
            double end = i == HISTOGRAM_BIN_COUNT - 1 ? max : start + binSize;
            boolean lastBin = i == HISTOGRAM_BIN_COUNT - 1;
            long count = values.stream()
                    .filter(value -> lastBin
                            ? value >= start && value <= end
                            : value >= start && value < end)
                    .count();
            bins.add(new GlobalHistogramBinResponse(round(start), round(end), count));
        }
        return bins;
    }

    private double round(double value)
    {
        return Double.parseDouble(String.format(Locale.US, "%.2f", value));
    }

    private final CategoryRepository categoryRepository;
    private final GlobalDatasetPointRepository globalDatasetPointRepository;
    private static final List<String> ALLOWED_FILTER_KEYS = List.of("sex", "age_group", "race_ethnicity", "region");
    private static final int HISTOGRAM_BIN_COUNT = 12;
}
