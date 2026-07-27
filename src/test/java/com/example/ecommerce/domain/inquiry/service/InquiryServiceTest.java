package com.example.ecommerce.domain.inquiry.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.example.ecommerce.domain.inquiry.dto.request.InquiryCreateRequest;
import com.example.ecommerce.domain.inquiry.entity.Inquiry;
import com.example.ecommerce.domain.inquiry.repository.InquiryRepository;
import com.example.ecommerce.domain.order.entity.Order;
import com.example.ecommerce.domain.order.entity.OrderItem;
import com.example.ecommerce.domain.order.service.OrderService;
import com.example.ecommerce.domain.product.entity.Product;
import com.example.ecommerce.domain.product.enums.ProductStatus;
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
class InquiryServiceTest {

    @Mock
    private InquiryRepository inquiryRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private OrderService orderService;

    @InjectMocks
    private InquiryService inquiryService;

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

    private Order buildOrder(Long id, User owner) {
        Product product = Product.builder()
                .name("탐미오족")
                .price(BigDecimal.valueOf(89000))
                .description("")
                .status(ProductStatus.ON_SALE)
                .build();
        ReflectionTestUtils.setField(product, "id", 10L);
        OrderItem item = OrderItem.builder()
                .product(product).orderPrice(product.getPrice()).quantity(1).build();
        Order order = Order.builder()
                .orderNumber("ORD-TEST-1")
                .user(owner)
                .totalAmount(product.getPrice())
                .discountAmount(BigDecimal.ZERO)
                .orderItems(new ArrayList<>(List.of(item)))
                .build();
        ReflectionTestUtils.setField(order, "id", id);
        return order;
    }

    @Test
    void createInquiry_succeeds_withoutOrder() {
        User user = buildUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(inquiryRepository.save(any(Inquiry.class))).thenAnswer(inv -> inv.getArgument(0));

        InquiryCreateRequest request = new InquiryCreateRequest("배송", "배송 문의", "언제 오나요?", null);

        var response = inquiryService.createInquiry(1L, request);

        assertThat(response.title()).isEqualTo("배송 문의");
        assertThat(response.orderId()).isNull();
    }

    @Test
    void createInquiry_succeeds_whenOrderOwnedByUser() {
        User user = buildUser(1L);
        Order order = buildOrder(1L, user);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(orderService.getOrderEntityOrThrow(1L)).thenReturn(order);
        when(inquiryRepository.save(any(Inquiry.class))).thenAnswer(inv -> inv.getArgument(0));

        InquiryCreateRequest request = new InquiryCreateRequest("주문", "주문 문의", "취소하고 싶어요", 1L);

        var response = inquiryService.createInquiry(1L, request);

        assertThat(response.orderId()).isEqualTo(1L);
    }

    @Test
    void createInquiry_throwsAccessDenied_whenOrderNotOwnedByUser() {
        User owner = buildUser(1L);
        Order order = buildOrder(1L, owner);
        when(userRepository.findById(2L)).thenReturn(Optional.of(buildUser(2L)));
        when(orderService.getOrderEntityOrThrow(1L)).thenReturn(order);

        InquiryCreateRequest request = new InquiryCreateRequest("주문", "주문 문의", "취소하고 싶어요", 1L);

        assertThatThrownBy(() -> inquiryService.createInquiry(2L, request))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ORDER_ACCESS_DENIED);
    }

    @Test
    void getMyInquiry_throwsAccessDenied_whenNotOwner() {
        User owner = buildUser(1L);
        Inquiry inquiry = Inquiry.builder()
                .user(owner).order(null).category("배송").title("배송 문의").content("언제 오나요?").build();
        ReflectionTestUtils.setField(inquiry, "id", 1L);
        when(inquiryRepository.findById(1L)).thenReturn(Optional.of(inquiry));

        assertThatThrownBy(() -> inquiryService.getMyInquiry(2L, 1L))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INQUIRY_ACCESS_DENIED);
    }
}
