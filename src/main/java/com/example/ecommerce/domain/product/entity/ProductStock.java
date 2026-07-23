package com.example.ecommerce.domain.product.entity;

import com.example.ecommerce.global.exception.CustomException;
import com.example.ecommerce.global.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "product_stocks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductStock {

    @Id
    @Column(name = "product_id")
    private Long productId;

    @OneToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "stock_quantity", nullable = false)
    private int stockQuantity;

    @Column(name = "reserved_quantity", nullable = false)
    private int reservedQuantity;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    private ProductStock(Product product, int stockQuantity) {
        this.product = product;
        this.stockQuantity = stockQuantity;
        this.reservedQuantity = 0;
    }

    public int getAvailableQuantity() {
        return stockQuantity - reservedQuantity;
    }

    public void reserve(int quantity) {
        if (getAvailableQuantity() < quantity) {
            throw new CustomException(ErrorCode.INSUFFICIENT_STOCK);
        }
        this.reservedQuantity += quantity;
    }

    public void releaseReservation(int quantity) {
        this.reservedQuantity = Math.max(0, this.reservedQuantity - quantity);
    }

    public void confirmDeduction(int quantity) {
        this.stockQuantity -= quantity;
        this.reservedQuantity = Math.max(0, this.reservedQuantity - quantity);
    }

    public void restore(int quantity) {
        this.stockQuantity += quantity;
    }

    @PreUpdate
    private void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
