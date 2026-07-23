package com.example.ecommerce.domain.order.entity;

import com.example.ecommerce.domain.product.entity.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "order_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_item_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "order_price", nullable = false)
    private BigDecimal orderPrice;

    @Column(nullable = false)
    private int quantity;

    @Builder
    private OrderItem(Product product, BigDecimal orderPrice, int quantity) {
        this.product = product;
        this.orderPrice = orderPrice;
        this.quantity = quantity;
    }

    void assignOrder(Order order) {
        this.order = order;
    }

    public BigDecimal getLineTotal() {
        return orderPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
