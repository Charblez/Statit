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
        return NhanesDatasetSeeder.HEIGHT_SOURCE_KEY;
    }

    @Override
    public GlobalDatasetSnapshot getSnapshot(Map<String, String> tags)
    {
        return globalDatasetPointService.buildSnapshot(
                sourceKey(),
                NhanesDatasetSeeder.NHANES_SOURCE_NAME + " - Height",
                NhanesDatasetSeeder.BODY_MEASURES_DOC_URL,
                tags
        );
    }

    private final GlobalDatasetPointService globalDatasetPointService;
}
