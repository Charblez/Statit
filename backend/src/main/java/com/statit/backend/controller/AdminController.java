/**
 * Filename: AdminController.java
 * Description: Admin-only routes for category moderation and score deletion.
 */

package com.statit.backend.controller;

import com.statit.backend.dto.CategoryCreateRequest;
import com.statit.backend.dto.CategoryListResponse;
import com.statit.backend.dto.CategoryResponse;
import com.statit.backend.dto.UserResponse;
import com.statit.backend.model.Category;
import com.statit.backend.model.User;
import com.statit.backend.service.AdminAuthService;
import com.statit.backend.service.CategoryService;
import com.statit.backend.service.ScoreService;
import com.statit.backend.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController
{
    public AdminController(CategoryService categoryService,
                           ScoreService scoreService,
                           UserService userService,
                           AdminAuthService adminAuthService)
    {
        this.categoryService = categoryService;
        this.scoreService = scoreService;
        this.userService = userService;
        this.adminAuthService = adminAuthService;
    }

    @GetMapping("/categories/pending")
    public ResponseEntity<CategoryListResponse> getPendingCategories(@RequestHeader("X-Admin-Username") String adminUsername,
                                                                     @RequestParam(defaultValue = "0") int page,
                                                                     @RequestParam(defaultValue = "25") int size)
    {
        adminAuthService.requireAdmin(adminUsername);

        Pageable pageable = PageRequest.of(page, size);
        Page<Category> categoryPage = categoryService.getPendingCategories(pageable);

        List<CategoryResponse> categories = new ArrayList<>();
        for(Category category : categoryPage.getContent())
        {
            categories.add(CategoryResponse.fromCategory(category, null));
        }

        return ResponseEntity.ok(new CategoryListResponse(
                categories,
                categoryPage.getNumber(),
                categoryPage.getTotalPages(),
                categoryPage.getTotalElements()
        ));
    }

    @GetMapping("/categories/{categoryId}")
    public ResponseEntity<CategoryResponse> getCategory(@RequestHeader("X-Admin-Username") String adminUsername,
                                                        @PathVariable UUID categoryId)
    {
        adminAuthService.requireAdmin(adminUsername);
        Category category = categoryService.getCategory(categoryId);
        return ResponseEntity.ok(CategoryResponse.fromCategory(category, null));
    }

    @PutMapping("/categories/{categoryId}")
    public ResponseEntity<CategoryResponse> updateCategory(@RequestHeader("X-Admin-Username") String adminUsername,
                                                           @PathVariable UUID categoryId,
                                                           @RequestBody CategoryCreateRequest request)
    {
        adminAuthService.requireAdmin(adminUsername);

        Category updated = categoryService.updateCategory(
                categoryId,
                request.name(),
                request.description(),
                request.tags(),
                request.units(),
                request.sortOrder(),
                request.lowerLimit(),
                request.upperLimit(),
                request.imageData()
        );

        return ResponseEntity.ok(CategoryResponse.fromCategory(updated, "Category updated successfully"));
    }

    @PostMapping("/categories/{categoryId}/approve")
    public ResponseEntity<CategoryResponse> approveCategory(@RequestHeader("X-Admin-Username") String adminUsername,
                                                            @PathVariable UUID categoryId)
    {
        adminAuthService.requireAdmin(adminUsername);
        Category approved = categoryService.approveCategory(categoryId);
        return ResponseEntity.ok(CategoryResponse.fromCategory(approved, "Category approved"));
    }

    @DeleteMapping("/categories/{categoryId}")
    public ResponseEntity<CategoryResponse> deleteCategory(@RequestHeader("X-Admin-Username") String adminUsername,
                                                           @PathVariable UUID categoryId)
    {
        adminAuthService.requireAdmin(adminUsername);
        Category category = categoryService.getCategory(categoryId);
        String name = category.getName();
        categoryService.deleteCategory(categoryId);

        return ResponseEntity.ok(new CategoryResponse(
                categoryId, name, null, null, null, null, null, null, null, null, null,
                "Category deleted successfully"
        ));
    }

    @DeleteMapping("/scores/{scoreId}")
    public ResponseEntity<Void> deleteScore(@RequestHeader("X-Admin-Username") String adminUsername,
                                            @PathVariable UUID scoreId)
    {
        adminAuthService.requireAdmin(adminUsername);
        scoreService.deleteScore(scoreId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> searchUsers(@RequestHeader("X-Admin-Username") String adminUsername,
                                                          @RequestParam(required = false) String query)
    {
        adminAuthService.requireAdmin(adminUsername);
        List<UserResponse> responses = new ArrayList<>();
        for(User user : userService.searchUsers(query))
        {
            responses.add(UserResponse.fromUser(user, null));
        }
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/users/{username}/grant-admin")
    public ResponseEntity<UserResponse> grantAdmin(@RequestHeader("X-Admin-Username") String adminUsername,
                                                   @PathVariable String username)
    {
        adminAuthService.requireAdmin(adminUsername);
        User updated = userService.grantAdmin(username);
        return ResponseEntity.ok(UserResponse.fromUser(updated, "Admin granted"));
    }

    private final CategoryService categoryService;
    private final ScoreService scoreService;
    private final UserService userService;
    private final AdminAuthService adminAuthService;
}
