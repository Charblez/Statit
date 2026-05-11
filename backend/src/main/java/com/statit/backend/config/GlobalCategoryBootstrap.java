package com.statit.backend.config;

import com.statit.backend.service.CategoryService;
import com.statit.backend.service.NhanesDatasetSeeder;
import com.statit.backend.service.OwidHeightDatasetSeeder;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(1)
public class GlobalCategoryBootstrap implements CommandLineRunner
{
    public GlobalCategoryBootstrap(CategoryService categoryService)
    {
        this.categoryService = categoryService;
    }

    @Override
    public void run(String... args)
    {
        categoryService.deleteGlobalCategoryBySourceKey("typing_speed_wpm");

        categoryService.ensureGlobalCategory(
                "Height",
                "Compare your height against a reference population.",
                "cm",
                List.of("global", "health"),
                true,
                50.0,
                250.0,
                OwidHeightDatasetSeeder.HEIGHT_SOURCE_KEY
        );

        categoryService.ensureGlobalCategory(
                "Weight",
                "Compare your weight against CDC NHANES row-level body-measures data. Source: CDC NHANES",
                "kg",
                List.of("global", "health", "nhanes"),
                true,
                1.0,
                300.0,
                NhanesDatasetSeeder.WEIGHT_SOURCE_KEY
        );

        categoryService.ensureGlobalCategory(
                "BMI",
                "Compare your BMI against CDC NHANES row-level body-measures data. Source: CDC NHANES",
                "kg/m^2",
                List.of("global", "health", "nhanes"),
                true,
                5.0,
                80.0,
                NhanesDatasetSeeder.BMI_SOURCE_KEY
        );
    }

    private final CategoryService categoryService;
}
