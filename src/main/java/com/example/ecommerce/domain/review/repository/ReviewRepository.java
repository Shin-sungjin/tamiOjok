package com.example.ecommerce.domain.review.repository;

import com.example.ecommerce.domain.order.entity.Order;
import com.example.ecommerce.domain.product.entity.Product;
import com.example.ecommerce.domain.review.entity.Review;
import com.example.ecommerce.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Page<Review> findByUser(User user, Pageable pageable);

    Page<Review> findByProduct(Product product, Pageable pageable);

    boolean existsByOrderAndProduct(Order order, Product product);
}
