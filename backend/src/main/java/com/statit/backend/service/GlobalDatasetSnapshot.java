package com.statit.backend.service;

import com.statit.backend.dto.GlobalHistogramBinResponse;
import com.statit.backend.dto.GlobalScatterPointResponse;

import java.util.List;

public record GlobalDatasetSnapshot(String sourceName,
                                    String sourceUrl,
                                    Double mean,
                                    Double standardDeviation,
                                    Integer sampleSize,
                                    List<Double> values,
                                    List<GlobalHistogramBinResponse> histogram,
                                    List<GlobalScatterPointResponse> scatterPoints)
{
}
