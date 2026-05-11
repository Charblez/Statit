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
import com.statit.backend.model.GlobalBaseline;
import com.statit.backend.model.User;
import com.statit.backend.repository.CategoryRepository;
import com.statit.backend.repository.GlobalBaselineRepository;
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
                           ScoreRepository scoreRepository)
    {
        this.categoryRepository = categoryRepository;
        this.globalBaselineRepository = globalBaselineRepository;
        this.scoreRepository = scoreRepository;
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

    @Transactional
    public Category approveCategory(UUID categoryId)
    {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found."));
        category.setLive(true);
        Category saved = categoryRepository.save(category);
        ensureGlobalBaseline(saved);
        return saved;
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

    //------------------------------------------------------------------------------------------------
    // Private Variables
    //------------------------------------------------------------------------------------------------
    private final CategoryRepository categoryRepository;
    private final GlobalBaselineRepository globalBaselineRepository;
    private final ScoreRepository scoreRepository;
}
