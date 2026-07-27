package com.example.ecommerce.domain.order.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ecommerce.domain.order.dto.request.OrderCreateRequest;
import com.example.ecommerce.domain.order.dto.response.OrderResponse;
import com.example.ecommerce.domain.order.entity.Order;
import com.example.ecommerce.domain.order.entity.OrderItem;
import com.example.ecommerce.domain.order.service.OrderService;
import com.example.ecommerce.domain.product.entity.Product;
import com.example.ecommerce.domain.product.enums.ProductStatus;
import com.example.ecommerce.domain.user.entity.User;
import com.example.ecommerce.domain.user.enums.Provider;
import com.example.ecommerce.domain.user.enums.Role;
import com.example.ecommerce.domain.user.enums.UserStatus;
import com.example.ecommerce.global.exception.CustomException;
import com.example.ecommerce.global.exception.ErrorCode;
import com.example.ecommerce.support.TestAuthentication;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    private User testUser(Long id) {
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

    private Order sampleOrder(Long id) {
        Product product = Product.builder()
                .name("탐미오족")
                .price(BigDecimal.valueOf(10000))
                .description("")
                .status(ProductStatus.ON_SALE)
                .build();
        ReflectionTestUtils.setField(product, "id", 10L);

        OrderItem item = OrderItem.builder()
                .product(product)
                .orderPrice(BigDecimal.valueOf(10000))
                .quantity(2)
                .build();

        Order order = Order.builder()
                .orderNumber("ORD-TEST-1")
                .user(testUser(1L))
                .totalAmount(BigDecimal.valueOf(20000))
                .discountAmount(BigDecimal.ZERO)
                .orderItems(new ArrayList<>(List.of(item)))
                .build();
        ReflectionTestUtils.setField(order, "id", id);
        return order;
    }

    @Test
    void createOrder_returns201_whenAuthenticated() throws Exception {
        when(orderService.createOrder(eq(1L), any())).thenReturn(OrderResponse.from(sampleOrder(1L)));

        OrderCreateRequest request = new OrderCreateRequest(
                List.of(new OrderCreateRequest.OrderItemRequest(10L, 2)), null);

        mockMvc.perform(post("/api/v1/orders")
                        .with(TestAuthentication.asUser(testUser(1L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalAmount").value(20000))
                .andExpect(jsonPath("$.items.length()").value(1));
    }

    @Test
    void createOrder_returns401_whenNotAuthenticated() throws Exception {
        OrderCreateRequest request = new OrderCreateRequest(
                List.of(new OrderCreateRequest.OrderItemRequest(10L, 2)), null);

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createOrder_returns403_whenCouponNotOwnedByUser() throws Exception {
        when(orderService.createOrder(eq(1L), any()))
                .thenThrow(new CustomException(ErrorCode.COUPON_ACCESS_DENIED));

        OrderCreateRequest request = new OrderCreateRequest(
                List.of(new OrderCreateRequest.OrderItemRequest(10L, 2)), 99L);

        mockMvc.perform(post("/api/v1/orders")
                        .with(TestAuthentication.asUser(testUser(1L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("COUPON_ACCESS_DENIED"));
    }
}
