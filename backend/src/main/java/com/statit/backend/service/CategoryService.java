/**
 * Filename: CategoryService.java
 * Author: Charles Bassani
 * Description: Handles CRUD operations for Categories
 */

//----------------------------------------------------------------------------------------------------
// Package
//----------------------------------------------------------------------------------------------------
package com.statit.backend.service;

//----------------------------------------------------------------------------------------------------
// Imports
//----------------------------------------------------------------------------------------------------
import com.statit.backend.model.Category;
import com.statit.backend.model.CategoryScope;
import com.statit.backend.model.GlobalBaseline;
import com.statit.backend.model.User;
import com.statit.backend.repository.CategoryRepository;
import com.statit.backend.repository.GlobalBaselineRepository;
import com.statit.backend.repository.GlobalDatasetPointRepository;
import com.statit.backend.repository.ScoreRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

//----------------------------------------------------------------------------------------------------
// Class Definition
//----------------------------------------------------------------------------------------------------
@Service
public class CategoryService
{
    //------------------------------------------------------------------------------------------------
    // Constructors
    //------------------------------------------------------------------------------------------------
    public CategoryService(CategoryRepository categoryRepository,
                           GlobalBaselineRepository globalBaselineRepository,
                           ScoreRepository scoreRepository,
                           GlobalDatasetPointRepository globalDatasetPointRepository)
    {
        this.categoryRepository = categoryRepository;
        this.globalBaselineRepository = globalBaselineRepository;
        this.scoreRepository = scoreRepository;
        this.globalDatasetPointRepository = globalDatasetPointRepository;
    }

    //------------------------------------------------------------------------------------------------
    // Public Methods
    //------------------------------------------------------------------------------------------------
    @Transactional
    public Category createCategory(String name,
                                   String description,
                                   String units,
                                   List<String> tags,
                                   Boolean sortOrder,
                                   Double lowerLimit,
                                   Double upperLimit,
                                   User foundingUser)
    {
        return createCategory(
                name,
                description,
                units,
                tags,
                sortOrder,
                lowerLimit,
                upperLimit,
                null,
                foundingUser
        );
    }

    @Transactional
    public Category createCategory(String name,
                                   String description,
                                   String units,
                                   List<String> tags,
                                   Boolean sortOrder,
                                   Double lowerLimit,
                                   Double upperLimit,
                                   String imageData,
                                   User foundingUser)
    {
        if (lowerLimit == null || upperLimit == null) {
            throw new IllegalArgumentException("Lower limit and upper limit are required.");
        }
        
        if(lowerLimit > upperLimit) {
            throw new IllegalArgumentException("Lower limit cannot be greater than upper limit.");
        }
        // ----------------------

        //Check if category exists already
        if(categoryRepository.findByCategoryName(name).isPresent())
        {
            throw new IllegalArgumentException("Category already exists.");
        }

        String normalizedImageData = normalizeImageData(imageData);

        //Create and save the category
        Category category = new Category(
                name,
                description,
                units,
                tags,
                sortOrder,
                foundingUser,
                lowerLimit,
                upperLimit,
                normalizedImageData
        );
        category.setCategoryScope(CategoryScope.LOCAL);
        category.setGlobalSourceKey(null);

        return categoryRepository.save(category);
    }

    @Transactional
    public Category updateCategory(UUID categoryId,
                                   String name,
                                   String description,
                                   List<String> tags,
                                   String units,
                                   Boolean sortOrder,
                                   Double lowerLimit,
                                   Double upperLimit)
    {
        return updateCategory(categoryId, name, description, tags, units, sortOrder, lowerLimit, upperLimit, null);
    }

    @Transactional
    public Category updateCategory(UUID categoryId,
                                   String name,
                                   String description,
                                   List<String> tags,
                                   String units,
                                   Boolean sortOrder,
                                   Double lowerLimit,
                                   Double upperLimit,
                                   String imageData)
    {
        // --- ADD THIS CHECK ---
        if (lowerLimit == null || upperLimit == null) {
            throw new IllegalArgumentException("Lower limit and upper limit are required.");
        }
        
        if(lowerLimit > upperLimit) {
            throw new IllegalArgumentException("Lower limit cannot be greater than upper limit.");
        }
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found."));

        String normalizedImageData = imageData == null
                ? category.getImageData()
                : normalizeImageData(imageData);

        category.update(name, description, units, tags, sortOrder, lowerLimit, upperLimit, normalizedImageData);
        return categoryRepository.save(category);
    }
    
    public Category getCategory(UUID categoryId)
    {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found."));
    }

    public Category getLiveCategory(UUID categoryId)
    {
        Category category = getCategory(categoryId);
        if(!category.getLive())
        {
            throw new IllegalArgumentException("Category is pending admin approval.");
        }
        return category;
    }

    public Page<Category> getAllCategories(Pageable pageable)
    {
        return categoryRepository.findAllByLiveTrueOrderByCategoryNameAsc(pageable);
    }

    public Page<Category> getPendingCategories(Pageable pageable)
    {
        return categoryRepository.findAllByLiveFalseOrderByCreatedAtAsc(pageable);
    }

    public Page<Category> getGlobalCategories(Pageable pageable)
    {
        return categoryRepository.findAllByLiveTrueAndCategoryScopeOrderByCategoryNameAsc(CategoryScope.GLOBAL, pageable);
    }

    @Transactional
    public Category approveCategory(UUID categoryId)
    {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found."));
        category.setLive(true);
        Category saved = categoryRepository.save(category);
        if(!saved.isGlobal())
        {
            ensureGlobalBaseline(saved);
        }
        return saved;
    }

    @Transactional
    public Category ensureGlobalCategory(String name,
                                         String description,
                                         String units,
                                         List<String> tags,
                                         Boolean sortOrder,
                                         Double lowerLimit,
                                         Double upperLimit,
                                         String globalSourceKey)
    {
        String globalDescription = appendSourceAttribution(description, globalSourceKey);
        Category category = categoryRepository.findByCategoryName(name).orElseGet(() ->
                new Category(name, globalDescription, units, tags, sortOrder, null, lowerLimit, upperLimit)
        );

        category.update(name, globalDescription, units, tags, sortOrder, lowerLimit, upperLimit, category.getImageData());
        category.setCategoryScope(CategoryScope.GLOBAL);
        category.setGlobalSourceKey(globalSourceKey);
        category.setLive(true);
        return categoryRepository.save(category);
    }

    @Transactional
    public void deleteGlobalCategoryBySourceKey(String globalSourceKey)
    {
        categoryRepository.findByGlobalSourceKey(globalSourceKey).ifPresent(category ->
                deleteCategory(category.getCategoryId())
        );
    }

    @Transactional
    public void deleteCategory(UUID categoryId)
    {
        //Get the category to delete
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found."));

        //Delete all scores in category
        scoreRepository.deleteAllByCategory(category);

        //Delete all baselines in category
        globalBaselineRepository.deleteAllByCategory(category);

        //Delete external reference points for global categories
        globalDatasetPointRepository.deleteAllByCategory(category);

        //Delete the category itself
        categoryRepository.delete(category);
    }

    //------------------------------------------------------------------------------------------------
    // Private Methods
    //------------------------------------------------------------------------------------------------
    private void ensureGlobalBaseline(Category category)
    {
        if(globalBaselineRepository.findByCategory(category).isPresent()) return;
        generateAndSaveGlobalBaseline(category);
    }

    private void generateAndSaveGlobalBaseline(Category category)
    {
        GlobalBaseline baseline = new GlobalBaseline(
                category,
                new HashMap<>(),
                0.0f,
                0.0f,
                0.0f,
                null,
                null,
                null,
                0,
                "My Global Ranking Team"
        );

        globalBaselineRepository.save(baseline);
    }

    private String normalizeImageData(String imageData)
    {
        if(imageData == null) return null;

        String normalized = imageData.trim();
        if(normalized.isEmpty()) return null;

        return normalized;
    }

    private String appendSourceAttribution(String description, String globalSourceKey)
    {
        if(globalSourceKey == null || !globalSourceKey.startsWith("nhanes_")) return description;
        String baseDescription = description != null ? description.trim() : "";
        if(baseDescription.toLowerCase().contains("source: cdc nhanes")) return baseDescription;
        if(baseDescription.isEmpty()) return "Source: CDC NHANES";
        return baseDescription + " Source: CDC NHANES";
    }

    //------------------------------------------------------------------------------------------------
    // Private Variables
    //------------------------------------------------------------------------------------------------
    private final CategoryRepository categoryRepository;
    private final GlobalBaselineRepository globalBaselineRepository;
    private final ScoreRepository scoreRepository;
    private final GlobalDatasetPointRepository globalDatasetPointRepository;
    private static final String CATEGORY_IMAGE_DIMENSIONS_ERROR = "Image must be exactly 512x512 pixels.";
}
