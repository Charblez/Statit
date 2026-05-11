package com.statit.backend.dto;

import java.util.UUID;

public record CorrelationPointResponse(UUID userId,
                                       Double primaryScore,
                                       Double secondaryScore)
{
}
