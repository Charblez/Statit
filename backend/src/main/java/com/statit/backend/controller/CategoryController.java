/**
 * Filename: CategoryController.java
 * Author: Wilson Jimenez
 * Description: API controller for category CRUD operations.
 */

//----------------------------------------------------------------------------------------------------
// Package
//----------------------------------------------------------------------------------------------------
package com.statit.backend.controller;

//----------------------------------------------------------------------------------------------------
// Imports
//----------------------------------------------------------------------------------------------------
import com.statit.backend.dto.CategoryCreateRequest;
import com.statit.backend.dto.CategoryListResponse;
import com.statit.backend.dto.CategoryResponse;
import com.statit.backend.model.Category;
import com.statit.backend.model.User;
import com.statit.backend.service.CategoryService;
import com.statit.backend.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

//----------------------------------------------------------------------------------------------------
// Class Definition
//----------------------------------------------------------------------------------------------------
@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController
{
    //------------------------------------------------------------------------------------------------
    // Constructors
    //------------------------------------------------------------------------------------------------
    public CategoryController(CategoryService categoryService,
                              UserService userService)
    {
        this.categoryService = categoryService;
        this.userService = userService;
    }

    //------------------------------------------------------------------------------------------------
    // Public Methods
    //------------------------------------------------------------------------------------------------
    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(@RequestBody CategoryCreateRequest request)
    {
        User foundingUser = userService.getUser(request.foundingUsername());
        List<String> tags = request.tags() != null ? request.tags() : new ArrayList<>();

        Category category = categoryService.createCategory(
                request.name(),
                request.description(),
                request.units(),
                tags,
                request.sortOrder(),
                request.lowerLimit(),
                request.upperLimit(),
                foundingUser
        );

        CategoryResponse response = CategoryResponse.fromCategory(category, "Category created successfully");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{categoryId}")
    public ResponseEntity<CategoryResponse> getCategory(@PathVariable UUID categoryId)
    {
        Category category = categoryService.getCategory(categoryId);
        CategoryResponse response = CategoryResponse.fromCategory(category, null);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<CategoryListResponse> getAllCategories(@RequestParam(defaultValue = "0") int page,
                                                                 @RequestParam(defaultValue = "25") int size)
    {
        Pageable pageable = PageRequest.of(page, size);
        Page<Category> categoryPage = categoryService.getAllCategories(pageable);

        List<CategoryResponse> categories = new ArrayList<>();
        for(Category category : categoryPage.getContent())
        {
            categories.add(CategoryResponse.fromCategory(category, null));
        }

        CategoryListResponse response = new CategoryListResponse(
                categories,
                categoryPage.getNumber(),
                categoryPage.getTotalPages(),
                categoryPage.getTotalElements()
        );

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{categoryId}")
    public ResponseEntity<CategoryResponse> updateCategory(@PathVariable UUID categoryId,
                                                           @RequestBody CategoryCreateRequest request)
    {
        Category updated = categoryService.updateCategory(
                categoryId,
                request.name(),
                request.description(),
                request.tags(),
                request.units(),
                request.sortOrder(),
                request.lowerLimit(),
                request.upperLimit()
        );

        CategoryResponse response = CategoryResponse.fromCategory(updated, "Category updated successfully");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{categoryId}")
    public ResponseEntity<CategoryResponse> deleteCategory(@PathVariable UUID categoryId)
    {
        Category category = categoryService.getCategory(categoryId);
        String name = category.getName();
        categoryService.deleteCategory(categoryId);

        // Added two extra nulls for lowerLimit and upperLimit
        return ResponseEntity.ok(new CategoryResponse(
                categoryId, name, null, null, null, null, null, null, null,
                "Category deleted successfully"
        ));
    }

    //------------------------------------------------------------------------------------------------
    // Private Variables
    //------------------------------------------------------------------------------------------------
    private final CategoryService categoryService;
    private final UserService userService;
}
