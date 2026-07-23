package com.example.ecommerce.domain.cart.entity;

import com.example.ecommerce.domain.product.entity.Product;
import com.example.ecommerce.domain.user.entity.User;
import com.example.ecommerce.global.exception.CustomException;
import com.example.ecommerce.global.exception.ErrorCode;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "carts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cart_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CartItem> cartItems = new ArrayList<>();

    @Builder
    private Cart(User user) {
        this.user = user;
    }

    public void addItem(Product product, int quantity) {
        findItemByProduct(product.getId())
                .ifPresentOrElse(
                        item -> item.increaseQuantity(quantity),
                        () -> cartItems.add(CartItem.builder().cart(this).product(product).quantity(quantity).build()));
    }

    public void changeItemQuantity(Long cartItemId, int quantity) {
        getItemOrThrow(cartItemId).changeQuantity(quantity);
    }

    public void removeItem(Long cartItemId) {
        CartItem item = getItemOrThrow(cartItemId);
        cartItems.remove(item);
    }

    public void clear() {
        cartItems.clear();
    }

    public BigDecimal getTotalAmount() {
        return cartItems.stream()
                .map(CartItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Optional<CartItem> findItemByProduct(Long productId) {
        return cartItems.stream().filter(item -> item.isForProduct(productId)).findFirst();
    }

    private CartItem getItemOrThrow(Long cartItemId) {
        return cartItems.stream()
                .filter(item -> item.getId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.CART_ITEM_NOT_FOUND));
    }
}
