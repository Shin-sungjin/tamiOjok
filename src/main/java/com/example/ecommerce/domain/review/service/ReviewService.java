package com.example.ecommerce.domain.review.service;

import com.example.ecommerce.domain.delivery.entity.Delivery;
import com.example.ecommerce.domain.delivery.enums.DeliveryStatus;
import com.example.ecommerce.domain.delivery.repository.DeliveryRepository;
import com.example.ecommerce.domain.order.entity.Order;
import com.example.ecommerce.domain.order.repository.OrderRepository;
import com.example.ecommerce.domain.product.entity.Product;
import com.example.ecommerce.domain.product.repository.ProductRepository;
import com.example.ecommerce.domain.review.dto.request.ReviewCreateRequest;
import com.example.ecommerce.domain.review.dto.request.ReviewUpdateRequest;
import com.example.ecommerce.domain.review.dto.response.ReviewResponse;
import com.example.ecommerce.domain.review.entity.Review;
import com.example.ecommerce.domain.review.repository.ReviewRepository;
import com.example.ecommerce.domain.user.entity.User;
import com.example.ecommerce.domain.user.repository.UserRepository;
import com.example.ecommerce.global.exception.CustomException;
import com.example.ecommerce.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final DeliveryRepository deliveryRepository;

    @Transactional
    public ReviewResponse createReview(Long userId, ReviewCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Order order = orderRepository.findById(request.orderId())
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));
        if (!order.isOwnedBy(userId)) {
            throw new CustomException(ErrorCode.ORDER_ACCESS_DENIED);
        }

        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        boolean orderedThisProduct = order.getOrderItems().stream()
                .anyMatch(item -> item.getProduct().getId().equals(product.getId()));
        boolean isDelivered = deliveryRepository.findByOrder(order)
                .map(Delivery::getStatus)
                .filter(status -> status == DeliveryStatus.DELIVERED)
                .isPresent();
        if (!orderedThisProduct || !isDelivered) {
            throw new CustomException(ErrorCode.REVIEW_NOT_ALLOWED);
        }

        if (reviewRepository.existsByOrderAndProduct(order, product)) {
            throw new CustomException(ErrorCode.DUPLICATE_REVIEW);
        }

        Review review = Review.builder()
                .user(user)
                .order(order)
                .product(product)
                .rating(request.rating())
                .content(request.content())
                .build();

        return ReviewResponse.from(reviewRepository.save(review));
    }

    @Transactional
    public ReviewResponse updateReview(Long userId, Long reviewId, ReviewUpdateRequest request) {
        Review review = getReviewOrThrow(reviewId);
        validateOwnership(review, userId);
        review.update(request.rating(), request.content());
        return ReviewResponse.from(review);
    }

    @Transactional
    public void deleteReview(Long userId, Long reviewId) {
        Review review = getReviewOrThrow(reviewId);
        validateOwnership(review, userId);
        reviewRepository.delete(review);
    }

    public Page<ReviewResponse> getMyReviews(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        return reviewRepository.findByUser(user, pageable).map(ReviewResponse::from);
    }

    public Page<ReviewResponse> getProductReviews(Long productId, Pageable pageable) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
        return reviewRepository.findByProduct(product, pageable).map(ReviewResponse::from);
    }

    private Review getReviewOrThrow(Long reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_NOT_FOUND));
    }

    private void validateOwnership(Review review, Long userId) {
        if (!review.isOwnedBy(userId)) {
            throw new CustomException(ErrorCode.REVIEW_ACCESS_DENIED);
        }
    }
}
