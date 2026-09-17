package com.akgeneralstore.repository;

import com.akgeneralstore.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    @Override
    @EntityGraph(attributePaths = "categories")
    Page<Product> findAll(Pageable pageable);

    List<Product> findByCategoryId(Long categoryId);
    List<Product> findByCategoryId(Long categoryId, Pageable pageable);
    List<Product> findByCategories_Id(Long categoryId, Pageable pageable);

    @EntityGraph(attributePaths = "categories")
    List<Product> findByIdNotIn(List<Long> ids, Pageable pageable);

    Optional<Product> findBySlug(String slug);
}
