package com.example.ecommerce.domain.product.repository;

import com.example.ecommerce.domain.product.entity.Product;
import com.example.ecommerce.domain.product.entity.ProductImage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    List<ProductImage> findByProductOrderBySortOrderAsc(Product product);

    List<ProductImage> findByProductInOrderByProductIdAscSortOrderAsc(List<Product> products);

    void deleteByProduct(Product product);
}
