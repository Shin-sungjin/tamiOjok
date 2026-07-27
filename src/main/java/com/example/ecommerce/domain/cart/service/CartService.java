package com.example.ecommerce.domain.cart.service;

import com.example.ecommerce.domain.cart.dto.request.CartItemAddRequest;
import com.example.ecommerce.domain.cart.dto.response.CartResponse;
import com.example.ecommerce.domain.cart.entity.Cart;
import com.example.ecommerce.domain.cart.repository.CartRepository;
import com.example.ecommerce.domain.product.entity.Product;
import com.example.ecommerce.domain.product.entity.ProductStock;
import com.example.ecommerce.domain.product.repository.ProductRepository;
import com.example.ecommerce.domain.product.repository.ProductStockRepository;
import com.example.ecommerce.domain.user.entity.User;
import com.example.ecommerce.domain.user.repository.UserRepository;
import com.example.ecommerce.global.exception.CustomException;
import com.example.ecommerce.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartService {

    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductStockRepository productStockRepository;

    public CartResponse getMyCart(Long userId) {
        return cartRepository.findByUser_Id(userId)
                .map(CartResponse::from)
                .orElseGet(CartResponse::empty);
    }

    @Transactional
    public CartResponse addItem(Long userId, CartItemAddRequest request) {
        Cart cart = cartRepository.findByUser_Id(userId).orElseGet(() -> createCart(userId));
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        // addItem은 같은 상품이 이미 담겨 있으면 기존 수량에 더하는 방식이라,
        // 검증도 "이미 담긴 수량 + 새로 담을 수량"의 합계 기준으로 해야 함.
        int resultingQuantity = cart.getQuantityForProduct(request.productId()) + request.quantity();
        validateAvailableStock(request.productId(), resultingQuantity);

        cart.addItem(product, request.quantity());
        return CartResponse.from(cart);
    }

    @Transactional
    public CartResponse updateItemQuantity(Long userId, Long cartItemId, int quantity) {
        Cart cart = getCartOrThrow(userId);
        Long productId = cart.getItemOrThrow(cartItemId).getProduct().getId();
        validateAvailableStock(productId, quantity);

        cart.changeItemQuantity(cartItemId, quantity);
        return CartResponse.from(cart);
    }

    private void validateAvailableStock(Long productId, int requestedQuantity) {
        ProductStock stock = productStockRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
        if (requestedQuantity > stock.getAvailableQuantity()) {
            throw new CustomException(ErrorCode.INSUFFICIENT_STOCK);
        }
    }

    @Transactional
    public void removeItem(Long userId, Long cartItemId) {
        Cart cart = getCartOrThrow(userId);
        cart.removeItem(cartItemId);
    }

    @Transactional
    public void clearCart(Long userId) {
        Cart cart = getCartOrThrow(userId);
        cart.clear();
    }

    private Cart createCart(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        return cartRepository.save(Cart.builder().user(user).build());
    }

    private Cart getCartOrThrow(Long userId) {
        return cartRepository.findByUser_Id(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.CART_ITEM_NOT_FOUND));
    }
}
