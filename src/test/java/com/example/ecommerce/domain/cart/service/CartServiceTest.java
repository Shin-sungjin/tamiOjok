package com.example.ecommerce.domain.cart.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.example.ecommerce.domain.cart.dto.request.CartItemAddRequest;
import com.example.ecommerce.domain.cart.dto.response.CartResponse;
import com.example.ecommerce.domain.cart.entity.Cart;
import com.example.ecommerce.domain.cart.repository.CartRepository;
import com.example.ecommerce.domain.product.entity.Product;
import com.example.ecommerce.domain.product.entity.ProductStock;
import com.example.ecommerce.domain.product.enums.ProductStatus;
import com.example.ecommerce.domain.product.repository.ProductRepository;
import com.example.ecommerce.domain.product.repository.ProductStockRepository;
import com.example.ecommerce.domain.user.entity.User;
import com.example.ecommerce.domain.user.enums.Provider;
import com.example.ecommerce.domain.user.enums.Role;
import com.example.ecommerce.domain.user.enums.UserStatus;
import com.example.ecommerce.domain.user.repository.UserRepository;
import com.example.ecommerce.global.exception.CustomException;
import com.example.ecommerce.global.exception.ErrorCode;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

// 장바구니는 재고를 예약하지 않고(예약은 체크아웃 시점) 담기만 하는 도메인이라,
// 재고보다 많은 수량을 담는 걸 막는 검증이 아예 없었음 — 이 테스트는 그 검증이
// 제대로 걸리는지 확인한다.
@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private ProductStockRepository productStockRepository;

    @InjectMocks
    private CartService cartService;

    private Product product(Long id) {
        Product product = Product.builder()
                .name("탐미오족")
                .price(BigDecimal.valueOf(89000))
                .description("")
                .status(ProductStatus.ON_SALE)
                .build();
        ReflectionTestUtils.setField(product, "id", id);
        return product;
    }

    private ProductStock stock(Product product, int stockQuantity) {
        return ProductStock.builder().product(product).stockQuantity(stockQuantity).build();
    }

    private Cart emptyCart() {
        User user = User.builder()
                .email("buyer@test.com")
                .name("구매자")
                .provider(Provider.LOCAL)
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build();
        return Cart.builder().user(user).build();
    }

    @Test
    void addItem_withinAvailableStock_succeeds() {
        Product product = product(1L);
        Cart cart = emptyCart();
        when(cartRepository.findByUser_Id(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productStockRepository.findById(1L)).thenReturn(Optional.of(stock(product, 10)));

        CartResponse response = cartService.addItem(1L, new CartItemAddRequest(1L, 10));

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).quantity()).isEqualTo(10);
    }

    @Test
    void addItem_exceedingAvailableStock_throwsInsufficientStock() {
        Product product = product(1L);
        Cart cart = emptyCart();
        when(cartRepository.findByUser_Id(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productStockRepository.findById(1L)).thenReturn(Optional.of(stock(product, 10)));

        assertThatThrownBy(() -> cartService.addItem(1L, new CartItemAddRequest(1L, 300)))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INSUFFICIENT_STOCK);
        assertThat(cart.getCartItems()).isEmpty();
    }

    @Test
    void addItem_alreadyInCart_validatesCombinedQuantityAgainstStock() {
        // 이미 8개가 담겨 있는 상태에서 5개를 더 담으려 하면(합계 13개), 재고 10개를
        // 초과하므로 막혀야 함 — "새로 담는 수량"만 보고 통과시키면 안 됨.
        Product product = product(1L);
        Cart cart = emptyCart();
        cart.addItem(product, 8);
        when(cartRepository.findByUser_Id(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productStockRepository.findById(1L)).thenReturn(Optional.of(stock(product, 10)));

        assertThatThrownBy(() -> cartService.addItem(1L, new CartItemAddRequest(1L, 5)))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INSUFFICIENT_STOCK);
        assertThat(cart.getCartItems().get(0).getQuantity()).isEqualTo(8); // 변경 안 됨
    }

    @Test
    void updateItemQuantity_exceedingAvailableStock_throwsInsufficientStock() {
        Product product = product(1L);
        Cart cart = emptyCart();
        cart.addItem(product, 2);
        // JPA @GeneratedValue라 실제로는 저장 후 채워지는 id를 단위테스트에서 흉내냄
        ReflectionTestUtils.setField(cart.getCartItems().get(0), "id", 1L);
        when(cartRepository.findByUser_Id(1L)).thenReturn(Optional.of(cart));
        when(productStockRepository.findById(1L)).thenReturn(Optional.of(stock(product, 10)));

        assertThatThrownBy(() -> cartService.updateItemQuantity(1L, 1L, 50))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INSUFFICIENT_STOCK);
    }
}
