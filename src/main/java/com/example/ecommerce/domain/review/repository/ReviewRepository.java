package com.example.ecommerce.domain.review.repository;

import com.example.ecommerce.domain.order.entity.Order;
import com.example.ecommerce.domain.product.entity.Product;
import com.example.ecommerce.domain.review.entity.Review;
import com.example.ecommerce.domain.user.entity.User;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Page<Review> findByUser(User user, Pageable pageable);

    Page<Review> findByProduct(Product product, Pageable pageable);

    boolean existsByOrderAndProduct(Order order, Product product);

    @Query("""
            SELECT r.product.id AS productId, AVG(r.rating) AS averageRating, COUNT(r) AS reviewCount
            FROM Review r
            WHERE r.product.id IN :productIds
            GROUP BY r.product.id
            """)
    List<ProductRatingStats> findRatingStatsByProductIds(@Param("productIds") List<Long> productIds);

    interface ProductRatingStats {
        Long getProductId();
        Double getAverageRating();
        Long getReviewCount();
    }
}
