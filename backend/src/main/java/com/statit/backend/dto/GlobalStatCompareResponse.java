package com.statit.backend.dto;

import java.util.List;
import java.util.UUID;

public record GlobalStatCompareResponse(UUID categoryId,
                                        String categoryName,
                                        String units,
                                        Double submittedValue,
                                        Double percentile,
                                        Integer rankEstimate,
                                        Double zScore,
                                        Double mean,
                                        Double standardDeviation,
                                        Integer sampleSize,
                                        String sourceName,
                                        String sourceUrl,
                                        List<GlobalHistogramBinResponse> histogram,
                                        List<GlobalScatterPointResponse> scatterPoints,
                                        String message)
{
}
