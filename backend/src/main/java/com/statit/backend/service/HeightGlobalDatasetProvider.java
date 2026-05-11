package com.statit.backend.service;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class HeightGlobalDatasetProvider implements GlobalDatasetProvider
{
    public HeightGlobalDatasetProvider(GlobalDatasetPointService globalDatasetPointService)
    {
        this.globalDatasetPointService = globalDatasetPointService;
    }

    @Override
    public String sourceKey()
    {
        return OwidHeightDatasetSeeder.HEIGHT_SOURCE_KEY;
    }

    @Override
    public GlobalDatasetSnapshot getSnapshot(Map<String, String> tags)
    {
        return globalDatasetPointService.buildSnapshot(
                sourceKey(),
                OwidHeightDatasetSeeder.HEIGHT_SOURCE_NAME,
                OwidHeightDatasetSeeder.HEIGHT_DOC_URL,
                tags
        );
    }

    private final GlobalDatasetPointService globalDatasetPointService;
}
