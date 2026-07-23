package com.example.ecommerce.domain.product.service;

import com.example.ecommerce.domain.product.entity.ProductStock;
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
    }

    private ProductStock getStockForUpdate(Long productId) {
        return productStockRepository.findByProductIdForUpdate(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
    }
}
