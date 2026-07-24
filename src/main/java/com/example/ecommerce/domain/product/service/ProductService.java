package com.example.ecommerce.domain.product.service;

import com.example.ecommerce.domain.product.dto.request.ProductCreateRequest;
import com.example.ecommerce.domain.product.dto.request.ProductUpdateRequest;
import com.example.ecommerce.domain.product.dto.response.ProductResponse;
import com.example.ecommerce.domain.product.entity.Product;
import com.example.ecommerce.domain.product.entity.ProductImage;
import com.example.ecommerce.domain.product.entity.ProductStock;
import com.example.ecommerce.domain.product.enums.ProductStatus;
import com.example.ecommerce.domain.product.repository.ProductImageRepository;
import com.example.ecommerce.domain.product.repository.ProductRepository;
import com.example.ecommerce.domain.product.repository.ProductStockRepository;
import com.example.ecommerce.domain.review.repository.ReviewRepository;
import com.example.ecommerce.domain.review.repository.ReviewRepository.ProductRatingStats;
import com.example.ecommerce.global.exception.CustomException;
import com.example.ecommerce.global.exception.ErrorCode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductStockRepository productStockRepository;
    private final ProductImageRepository productImageRepository;
    private final ReviewRepository reviewRepository;

    @Transactional
    public Long createProduct(ProductCreateRequest request) {
        Product product = Product.builder()
                .name(request.name())
                .price(request.price())
                .description(request.description())
                .status(ProductStatus.ON_SALE)
                .build();
        productRepository.save(product);

        ProductStock stock = ProductStock.builder()
                .product(product)
                .stockQuantity(request.initialStock())
                .build();
        productStockRepository.save(stock);

        saveImages(product, request.imageUrls());

        return product.getId();
    }

    @Transactional
    public void updateProduct(Long productId, ProductUpdateRequest request) {
        Product product = getProductOrThrow(productId);
        product.update(request.name(), request.price(), request.description());

        productImageRepository.deleteByProduct(product);
        saveImages(product, request.imageUrls());
    }

    @Transactional
    public void changeStatus(Long productId, ProductStatus status) {
        Product product = getProductOrThrow(productId);
        product.changeStatus(status);
    }

    public ProductResponse getProduct(Long productId) {
        Product product = getProductOrThrow(productId);
        ProductStock stock = getStockOrThrow(productId);
        List<String> imageUrls = productImageRepository.findByProductOrderBySortOrderAsc(product).stream()
                .map(ProductImage::getImageUrl)
                .toList();
        ProductRatingStats stats = ratingStatsByProductId(List.of(productId)).get(productId);
        return ProductResponse.of(
                product, stock, imageUrls,
                stats != null ? stats.getAverageRating() : null,
                stats != null ? stats.getReviewCount() : 0);
    }

    public Page<ProductResponse> getOnSaleProducts(String keyword, Pageable pageable) {
        Page<Product> products = (keyword == null || keyword.isBlank())
                ? productRepository.findByStatus(ProductStatus.ON_SALE, pageable)
                : productRepository.findByStatusAndNameContainingIgnoreCase(
                        ProductStatus.ON_SALE, keyword.trim(), pageable);
        return mapWithImages(products);
    }

    public Page<ProductResponse> getProductsForAdmin(ProductStatus status, Pageable pageable) {
        Page<Product> products = status != null
                ? productRepository.findByStatus(status, pageable)
                : productRepository.findAll(pageable);
        return mapWithImages(products);
    }

    private Page<ProductResponse> mapWithImages(Page<Product> products) {
        Map<Long, List<String>> imagesByProductId =
                productImageRepository.findByProductInOrderByProductIdAscSortOrderAsc(products.getContent()).stream()
                        .collect(Collectors.groupingBy(
                                image -> image.getProduct().getId(),
                                Collectors.mapping(ProductImage::getImageUrl, Collectors.toList())));

        List<Long> productIds = products.getContent().stream().map(Product::getId).toList();
        Map<Long, ProductRatingStats> statsByProductId = ratingStatsByProductId(productIds);

        return products.map(product -> {
            ProductRatingStats stats = statsByProductId.get(product.getId());
            return ProductResponse.of(
                    product, getStockOrThrow(product.getId()), imagesByProductId.getOrDefault(product.getId(), List.of()),
                    stats != null ? stats.getAverageRating() : null,
                    stats != null ? stats.getReviewCount() : 0);
        });
    }

    private Map<Long, ProductRatingStats> ratingStatsByProductId(List<Long> productIds) {
        if (productIds.isEmpty()) {
            return Map.of();
        }
        return reviewRepository.findRatingStatsByProductIds(productIds).stream()
                .collect(Collectors.toMap(ProductRatingStats::getProductId, stats -> stats));
    }

    private void saveImages(Product product, List<String> imageUrls) {
        if (imageUrls == null) {
            return;
        }
        int order = 0;
        for (String imageUrl : imageUrls) {
            if (imageUrl == null || imageUrl.isBlank()) {
                continue;
            }
            productImageRepository.save(ProductImage.builder()
                    .product(product)
                    .imageUrl(imageUrl.trim())
                    .sortOrder(order++)
                    .build());
        }
    }

    private Product getProductOrThrow(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    private ProductStock getStockOrThrow(Long productId) {
        return productStockRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
    }
}
