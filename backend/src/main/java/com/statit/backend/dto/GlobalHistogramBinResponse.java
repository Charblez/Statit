package com.statit.backend.dto;

public record GlobalHistogramBinResponse(Double start,
                                         Double end,
                                         Long count)
{
}
