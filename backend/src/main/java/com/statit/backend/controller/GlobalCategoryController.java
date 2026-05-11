package com.statit.backend.controller;

import com.statit.backend.dto.CategoryListResponse;
import com.statit.backend.dto.CategoryResponse;
import com.statit.backend.dto.GlobalDatasetResponse;
import com.statit.backend.dto.GlobalStatCompareResponse;
import com.statit.backend.dto.GlobalStatSubmitRequest;
import com.statit.backend.model.Category;
import com.statit.backend.service.GlobalCategoryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/global-categories")
public class GlobalCategoryController
{
    public GlobalCategoryController(GlobalCategoryService globalCategoryService)
    {
        this.globalCategoryService = globalCategoryService;
    }

    @GetMapping
    public ResponseEntity<CategoryListResponse> getGlobalCategories(@RequestParam(defaultValue = "0") int page,
                                                                    @RequestParam(defaultValue = "25") int size)
    {
        Pageable pageable = PageRequest.of(page, size);
        Page<Category> categoryPage = globalCategoryService.getGlobalCategories(pageable);

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

    @GetMapping("/{categoryId}/dataset")
    public ResponseEntity<GlobalDatasetResponse> getDataset(@PathVariable UUID categoryId,
                                                            @RequestParam Map<String, String> filters)
    {
        return ResponseEntity.ok(globalCategoryService.getDataset(categoryId, filters));
    }

    @PostMapping("/{categoryId}/compare")
    public ResponseEntity<GlobalStatCompareResponse> compare(@PathVariable UUID categoryId,
                                                             @RequestParam Map<String, String> filters,
                                                             @RequestBody GlobalStatSubmitRequest request)
    {
        return ResponseEntity.ok(globalCategoryService.compare(categoryId, request.score(), filters));
    }

    private final GlobalCategoryService globalCategoryService;
}
