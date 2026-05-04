package com.statit.backend.controller;

import com.statit.backend.TestUtils;
import com.statit.backend.dto.CategoryCreateRequest;
import com.statit.backend.dto.CategoryListResponse;
import com.statit.backend.dto.CategoryResponse;
import com.statit.backend.model.Category;
import com.statit.backend.model.User;
import com.statit.backend.service.CategoryService;
import com.statit.backend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryControllerTest
{
    @Mock private CategoryService categoryService;
    @Mock private UserService userService;
    @InjectMocks private CategoryController categoryController;

    private User founder;
    private Category category;
    private UUID categoryId;

    @BeforeEach
    void setUp()
    {
        founder = new User("f", "f@x", "h", LocalDate.of(2000, 1, 1), null);
        TestUtils.setField(founder, "userId", UUID.randomUUID());

        category = new Category("Cat", "d", "u", null, true, founder);
        categoryId = UUID.randomUUID();
        TestUtils.setField(category, "categoryId", categoryId);
    }

    @Test
    void createCategoryReturnsOk()
    {
        CategoryCreateRequest req = new CategoryCreateRequest("Cat", "d", "u",
                Arrays.asList("a"), true, "f");
        when(userService.getUser("f")).thenReturn(founder);
        when(categoryService.createCategory(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(category);

        ResponseEntity<CategoryResponse> response = categoryController.createCategory(req);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Cat", response.getBody().name());
        assertEquals("Category submitted for admin approval", response.getBody().message());
    }

    @Test
    void createCategoryUsesEmptyListForNullTags()
    {
        CategoryCreateRequest req = new CategoryCreateRequest("Cat", "d", "u",
                null, true, "f");
        when(userService.getUser("f")).thenReturn(founder);
        when(categoryService.createCategory(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(category);

        ResponseEntity<CategoryResponse> response = categoryController.createCategory(req);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getCategoryReturnsOk()
    {
        when(categoryService.getLiveCategory(categoryId)).thenReturn(category);
        ResponseEntity<CategoryResponse> response = categoryController.getCategory(categoryId);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Cat", response.getBody().name());
    }

    @Test
    void getAllCategoriesReturnsPageInfo()
    {
        Pageable pageable = PageRequest.of(0, 25);
        Page<Category> page = new PageImpl<>(List.of(category), pageable, 1);
        when(categoryService.getAllCategories(any(Pageable.class))).thenReturn(page);

        ResponseEntity<CategoryListResponse> response = categoryController.getAllCategories(0, 25);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().categories().size());
        assertEquals(0, response.getBody().page());
        assertEquals(1, response.getBody().totalElements());
    }

}
