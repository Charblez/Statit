package com.statit.backend.dto;

import java.util.List;
import java.util.UUID;

public record CorrelationResponse(UUID primaryCategoryId,
                                  UUID secondaryCategoryId,
                                  String primaryCategoryName,
                                  String secondaryCategoryName,
                                  String primaryUnits,
                                  String secondaryUnits,
                                  Double pearsonCorrelation,
                                  Integer sampleSize,
                                  List<CorrelationPointResponse> points)
{
}
