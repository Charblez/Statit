package com.statit.backend.dto;

import java.util.List;
import java.util.UUID;

public record GlobalDatasetResponse(UUID categoryId,
                                    String categoryName,
                                    String units,
                                    String sourceName,
                                    String sourceUrl,
                                    Double mean,
                                    Double standardDeviation,
                                    Integer sampleSize,
                                    List<Double> values,
                                    List<GlobalHistogramBinResponse> histogram,
                                    List<GlobalScatterPointResponse> scatterPoints)
{
}
