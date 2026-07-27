package com.example.ecommerce.domain.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.ecommerce.domain.delivery.entity.Delivery;
import com.example.ecommerce.domain.delivery.repository.DeliveryRepository;
import com.example.ecommerce.domain.order.entity.Order;
import com.example.ecommerce.domain.order.entity.OrderItem;
import com.example.ecommerce.domain.order.repository.OrderRepository;
import com.example.ecommerce.domain.product.entity.Product;
import com.example.ecommerce.domain.product.enums.ProductStatus;
import com.example.ecommerce.domain.product.repository.ProductRepository;
import com.example.ecommerce.domain.review.dto.request.ReviewCreateRequest;
import com.example.ecommerce.domain.review.dto.request.ReviewUpdateRequest;
import com.example.ecommerce.domain.review.entity.Review;
import com.example.ecommerce.domain.review.repository.ReviewRepository;
import com.example.ecommerce.domain.user.entity.User;
import com.example.ecommerce.domain.user.enums.Provider;
import com.example.ecommerce.domain.user.enums.Role;
import com.example.ecommerce.domain.user.enums.UserStatus;
import com.example.ecommerce.domain.user.repository.UserRepository;
import com.example.ecommerce.global.exception.CustomException;
import com.example.ecommerce.global.exception.ErrorCode;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private DeliveryRepository deliveryRepository;

    @InjectMocks
    private ReviewService reviewService;

    private User buildUser(Long id) {
        User user = User.builder()
                .email("buyer@test.com")
                .name("구매자")
                .provider(Provider.LOCAL)
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Product buildProduct(Long id) {
        Product product = Product.builder()
                .name("탐미오족")
                .price(BigDecimal.valueOf(89000))
                .description("")
                .status(ProductStatus.ON_SALE)
                .build();
        ReflectionTestUtils.setField(product, "id", id);
        return product;
    }

    private Order buildOrder(Long id, User user, Product product) {
        OrderItem item = OrderItem.builder()
                .product(product)
                .orderPrice(product.getPrice())
                .quantity(1)
                .build();
        Order order = Order.builder()
                .orderNumber("ORD-TEST-1")
                .user(user)
                .totalAmount(product.getPrice())
                .discountAmount(BigDecimal.ZERO)
                .orderItems(new ArrayList<>(List.of(item)))
                .build();
        ReflectionTestUtils.setField(order, "id", id);
        return order;
    }

    private Delivery deliveredDelivery(Order order) {
        Delivery delivery = Delivery.builder()
                .order(order)
                .courierCode("CJGLS")
                .trackingNumber("1234567890")
                .build();
        delivery.markDelivered();
        return delivery;
    }

    @Test
    void createReview_succeeds_whenProductOrderedAndDelivered() {
        User user = buildUser(1L);
        Product product = buildProduct(10L);
        Order order = buildOrder(1L, user, product);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(deliveryRepository.findByOrder(order)).thenReturn(Optional.of(deliveredDelivery(order)));
        when(reviewRepository.existsByOrderAndProduct(order, product)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        ReviewCreateRequest request = new ReviewCreateRequest(1L, 10L, 5, "맛있어요");

        var response = reviewService.createReview(1L, request);

        assertThat(response.rating()).isEqualTo(5);
        assertThat(response.content()).isEqualTo("맛있어요");
    }

    @Test
    void createReview_throwsReviewNotAllowed_whenProductNotInOrder() {
        User user = buildUser(1L);
        Product orderedProduct = buildProduct(10L);
        Product otherProduct = buildProduct(20L);
        Order order = buildOrder(1L, user, orderedProduct);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(productRepository.findById(20L)).thenReturn(Optional.of(otherProduct));
        when(deliveryRepository.findByOrder(order)).thenReturn(Optional.of(deliveredDelivery(order)));

        ReviewCreateRequest request = new ReviewCreateRequest(1L, 20L, 5, "맛있어요");

        assertThatThrownBy(() -> reviewService.createReview(1L, request))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.REVIEW_NOT_ALLOWED);
    }

    @Test
    void createReview_throwsReviewNotAllowed_whenNotDelivered() {
        User user = buildUser(1L);
        Product product = buildProduct(10L);
        Order order = buildOrder(1L, user, product);
        Delivery inTransitDelivery = Delivery.builder()
                .order(order).courierCode("CJGLS").trackingNumber("1234567890").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(deliveryRepository.findByOrder(order)).thenReturn(Optional.of(inTransitDelivery));

        ReviewCreateRequest request = new ReviewCreateRequest(1L, 10L, 5, "맛있어요");

        assertThatThrownBy(() -> reviewService.createReview(1L, request))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.REVIEW_NOT_ALLOWED);
    }

    @Test
    void createReview_throwsDuplicateReview_whenAlreadyReviewed() {
        User user = buildUser(1L);
        Product product = buildProduct(10L);
        Order order = buildOrder(1L, user, product);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(deliveryRepository.findByOrder(order)).thenReturn(Optional.of(deliveredDelivery(order)));
        when(reviewRepository.existsByOrderAndProduct(order, product)).thenReturn(true);

        ReviewCreateRequest request = new ReviewCreateRequest(1L, 10L, 5, "맛있어요");

        assertThatThrownBy(() -> reviewService.createReview(1L, request))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_REVIEW);
    }

    @Test
    void updateReview_throwsAccessDenied_whenNotOwner() {
        User owner = buildUser(1L);
        Product product = buildProduct(10L);
        Order order = buildOrder(1L, owner, product);
        Review review = Review.builder().user(owner).order(order).product(product).rating(4).content("좋아요").build();
        ReflectionTestUtils.setField(review, "id", 1L);
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

        ReviewUpdateRequest request = new ReviewUpdateRequest(1, "별로예요");

        assertThatThrownBy(() -> reviewService.updateReview(2L, 1L, request))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.REVIEW_ACCESS_DENIED);
    }

    @Test
    void deleteReview_throwsAccessDenied_whenNotOwner() {
        User owner = buildUser(1L);
        Product product = buildProduct(10L);
        Order order = buildOrder(1L, owner, product);
        Review review = Review.builder().user(owner).order(order).product(product).rating(4).content("좋아요").build();
        ReflectionTestUtils.setField(review, "id", 1L);
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.deleteReview(2L, 1L))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.REVIEW_ACCESS_DENIED);

        verify(reviewRepository, never()).delete(any());
    }
}
