package com.example.ecommerce.domain.product.service;

import com.example.ecommerce.domain.product.entity.Product;
import com.example.ecommerce.domain.product.entity.ProductStock;
import com.example.ecommerce.domain.product.enums.ProductStatus;
import com.example.ecommerce.domain.product.repository.ProductStockRepository;
import com.example.ecommerce.global.exception.CustomException;
import com.example.ecommerce.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StockService {

    private final ProductStockRepository productStockRepository;

    @Transactional
    public void reserve(Long productId, int quantity) {
        ProductStock stock = getStockForUpdate(productId);
        stock.reserve(quantity);
    }

    @Transactional
    public void releaseReservation(Long productId, int quantity) {
        ProductStock stock = getStockForUpdate(productId);
        stock.releaseReservation(quantity);
    }

    @Transactional
    public void confirmDeduction(Long productId, int quantity) {
        ProductStock stock = getStockForUpdate(productId);
        stock.confirmDeduction(quantity);
    }

    @Transactional
    public void restoreStock(Long productId, int quantity) {
        ProductStock stock = getStockForUpdate(productId);
        stock.restore(quantity);
        reviveIfOutOfStock(stock);
    }

    // 관리자 입고(restock)든 주문 취소로 인한 재고 복원이든, 재고가 다시 생기면
    // 품절(OUT_OF_STOCK) 상태를 그대로 유지할 이유가 없음. 반대로 이 상태 전환은
    // status가 어디서도 자동으로 OUT_OF_STOCK이 되지 않는(수동 지정 외엔 seed
    // 데이터로만 존재) 것과는 별개로, 이미 OUT_OF_STOCK인 상품을 되살리는
    // 역할만 함 — 판매중지(HIDDEN)는 관리자의 의도적인 선택이라 건드리지 않음.
    private void reviveIfOutOfStock(ProductStock stock) {
        Product product = stock.getProduct();
        if (product.getStatus() == ProductStatus.OUT_OF_STOCK && stock.getAvailableQuantity() > 0) {
            product.changeStatus(ProductStatus.ON_SALE);
        }
    }

    private ProductStock getStockForUpdate(Long productId) {
        return productStockRepository.findByProductIdForUpdate(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
    }
}
