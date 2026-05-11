/**
 * Filename: CategoryRepository.java
 * Author: Charles Bassani
 * Description: Repository for Category table queries
 */

//----------------------------------------------------------------------------------------------------
// Package
//----------------------------------------------------------------------------------------------------
package com.statit.backend.repository;

//----------------------------------------------------------------------------------------------------
// Imports
//----------------------------------------------------------------------------------------------------
import com.statit.backend.model.Category;
import com.statit.backend.model.CategoryScope;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

//----------------------------------------------------------------------------------------------------
// Interface Definition
//----------------------------------------------------------------------------------------------------
@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID>
{
    //------------------------------------------------------------------------------------------------
    // Single Category Lookups
    //------------------------------------------------------------------------------------------------
    Optional<Category> findByCategoryName(String categoryName);

    Optional<Category> findByGlobalSourceKey(String globalSourceKey);

    //------------------------------------------------------------------------------------------------
    // Paginated Category Queries
    //------------------------------------------------------------------------------------------------
    Page<Category> findAllByOrderByCategoryNameAsc(Pageable pageable);

    Page<Category> findAllByLiveTrueOrderByCategoryNameAsc(Pageable pageable);

    Page<Category> findAllByLiveTrueAndCategoryScopeOrderByCategoryNameAsc(CategoryScope categoryScope, Pageable pageable);

    Page<Category> findAllByLiveFalseOrderByCreatedAtAsc(Pageable pageable);
}
