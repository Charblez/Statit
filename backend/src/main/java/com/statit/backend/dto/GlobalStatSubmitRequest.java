package com.statit.backend.dto;

import java.util.Map;

public record GlobalStatSubmitRequest(Double score,
                                      Map<String, String> tags)
{
}
