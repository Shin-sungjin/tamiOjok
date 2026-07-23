package com.example.ecommerce.domain.cart.service;

import com.example.ecommerce.domain.cart.dto.request.CartItemAddRequest;
import com.example.ecommerce.domain.cart.dto.response.CartResponse;
import com.example.ecommerce.domain.cart.entity.Cart;
import com.example.ecommerce.domain.cart.repository.CartRepository;
import com.example.ecommerce.domain.product.entity.Product;
import com.example.ecommerce.domain.product.repository.ProductRepository;
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

        cart.addItem(product, request.quantity());
        return CartResponse.from(cart);
    }

    @Transactional
    public CartResponse updateItemQuantity(Long userId, Long cartItemId, int quantity) {
        Cart cart = getCartOrThrow(userId);
        cart.changeItemQuantity(cartItemId, quantity);
        return CartResponse.from(cart);
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
