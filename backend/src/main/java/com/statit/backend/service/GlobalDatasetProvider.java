package com.statit.backend.service;

import java.util.Map;

public interface GlobalDatasetProvider
{
    String sourceKey();

    GlobalDatasetSnapshot getSnapshot(Map<String, String> tags);
}
