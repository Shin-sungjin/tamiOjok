package com.example.ecommerce.domain.product.repository;

import com.example.ecommerce.domain.product.entity.Product;
import com.example.ecommerce.domain.product.enums.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Page<Product> findByStatus(ProductStatus status, Pageable pageable);
}
