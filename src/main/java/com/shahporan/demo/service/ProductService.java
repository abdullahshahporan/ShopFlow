package com.shahporan.demo.service;

import com.shahporan.demo.dto.ProductRequestDto;
import com.shahporan.demo.dto.ProductResponseDto;
import com.shahporan.demo.entity.Product;
import com.shahporan.demo.entity.Seller;
import com.shahporan.demo.entity.Stock;
import com.shahporan.demo.exception.BadRequestException;
import com.shahporan.demo.exception.ResourceNotFoundException;
import com.shahporan.demo.repository.ProductRepository;
import com.shahporan.demo.repository.SellerRepository;
import com.shahporan.demo.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final SellerRepository sellerRepository;
    private final StockRepository stockRepository;

    @Transactional
    public ProductResponseDto createProduct(ProductRequestDto dto, Long sellerId) {
        validatePriceAndQuantity(dto.getPrice(), dto.getQuantity());

        if (productRepository.existsBySkuIgnoreCase(dto.getSku().trim())) {
            throw new BadRequestException("SKU already exists: " + dto.getSku());
        }

        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found with id: " + sellerId));

        Product product = Product.builder()
                .seller(seller)
                .name(dto.getName().trim())
                .sku(dto.getSku().trim())
                .price(dto.getPrice())
                .quantity(dto.getQuantity())
                .active(true)
                .build();

        Product savedProduct = productRepository.save(product);
        stockRepository.save(Stock.builder()
            .product(savedProduct)
            .seller(seller)
            .quantity(savedProduct.getQuantity())
            .build());

        return toResponse(savedProduct);
    }

    @Transactional
    public ProductResponseDto updateProduct(Long productId, ProductRequestDto dto, Long sellerId) {
        validatePriceAndQuantity(dto.getPrice(), dto.getQuantity());

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        verifySellerOwnership(product, sellerId);

        if (productRepository.existsBySkuIgnoreCaseAndIdNot(dto.getSku().trim(), productId)) {
            throw new BadRequestException("SKU already exists: " + dto.getSku());
        }

        product.setName(dto.getName().trim());
        product.setSku(dto.getSku().trim());
        product.setPrice(dto.getPrice());
        product.setQuantity(dto.getQuantity());
        if (dto.getActive() != null) {
            product.setActive(dto.getActive());
        }

        Product updatedProduct = productRepository.save(product);
        Stock stock = stockRepository.findByProductId(productId)
            .orElseGet(() -> Stock.builder()
                .product(updatedProduct)
                .seller(updatedProduct.getSeller())
                .quantity(0)
                .build());
        stock.setSeller(updatedProduct.getSeller());
        stock.setQuantity(updatedProduct.getQuantity());
        stockRepository.save(stock);

        return toResponse(updatedProduct);
    }

    @Transactional
    public void deleteProduct(Long productId, Long sellerId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        verifySellerOwnership(product, sellerId);
        product.setActive(false);
        productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public ProductResponseDto getProductById(Long productId) {
        Product product = productRepository.findByIdAndActiveTrue(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
        return toResponse(product);
    }

    @Transactional(readOnly = true)
    public List<ProductResponseDto> getAllActiveProducts() {
        return productRepository.findByActiveTrue().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductResponseDto> getProductsBySeller(Long sellerId) {
        return productRepository.findBySellerId(sellerId).stream().map(this::toResponse).toList();
    }

    private void verifySellerOwnership(Product product, Long sellerId) {
        if (product.getSeller() == null || !product.getSeller().getId().equals(sellerId)) {
            throw new AccessDeniedException("You do not own this product");
        }
    }

    private void validatePriceAndQuantity(BigDecimal price, Integer quantity) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Price must be greater than 0");
        }
        if (quantity == null || quantity < 0) {
            throw new BadRequestException("Quantity must be non-negative");
        }
    }

    private ProductResponseDto toResponse(Product product) {
        return ProductResponseDto.builder()
                .id(product.getId())
                .sellerId(product.getSeller() != null ? product.getSeller().getId() : null)
                .sellerName(product.getSeller() != null ? product.getSeller().getName() : null)
                .name(product.getName())
                .sku(product.getSku())
                .price(product.getPrice())
                .quantity(product.getQuantity())
                .active(Boolean.TRUE.equals(product.getActive()))
                .createdAt(product.getCreatedAt())
                .build();
    }
}
