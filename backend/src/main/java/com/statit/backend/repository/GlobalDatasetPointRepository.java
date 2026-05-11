package com.statit.backend.repository;

import com.statit.backend.model.Category;
import com.statit.backend.model.GlobalDatasetPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GlobalDatasetPointRepository extends JpaRepository<GlobalDatasetPoint, UUID>
{
    long countByCategory(Category category);

    void deleteAllByCategory(Category category);

    List<GlobalDatasetPoint> findAllByCategoryOrderByValueAsc(Category category);
}
