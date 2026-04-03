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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private SellerRepository sellerRepository;

    @Mock
    private StockRepository stockRepository;

    @InjectMocks
    private ProductService productService;

    private Seller seller;

    @BeforeEach
    void setUp() {
        seller = Seller.builder()
                .id(1L)
                .name("Test Seller")
                .email("seller@test.com")
                .passwordHash("hash")
                .enabled(true)
                .build();
    }

    private ProductRequestDto buildRequest(String name, String sku, String price, int qty) {
        ProductRequestDto dto = new ProductRequestDto();
        dto.setName(name);
        dto.setSku(sku);
        dto.setPrice(new BigDecimal(price));
        dto.setQuantity(qty);
        return dto;
    }

    private Product buildProduct(Long id) {
        return Product.builder()
                .id(id)
                .seller(seller)
                .name("Widget")
                .sku("SKU-001")
                .price(new BigDecimal("50.00"))
                .active(true)
                .build();
    }

    // -----------------------------------------------------------------------
    // createProduct
    // -----------------------------------------------------------------------

    @Test
    void createProduct_whenValid_thenReturnsMappedDto() {
        ProductRequestDto dto = buildRequest("Widget", "SKU-001", "50.00", 10);

        when(productRepository.existsBySkuIgnoreCase("SKU-001")).thenReturn(false);
        when(sellerRepository.findById(1L)).thenReturn(Optional.of(seller));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        when(stockRepository.save(any(Stock.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductResponseDto result = productService.createProduct(dto, 1L);

        assertThat(result.getName()).isEqualTo("Widget");
        assertThat(result.getSku()).isEqualTo("SKU-001");
        assertThat(result.getPrice()).isEqualByComparingTo("50.00");
        assertThat(result.isActive()).isTrue();
        verify(stockRepository).save(any(Stock.class));
    }

    @Test
    void createProduct_whenSkuAlreadyExists_thenThrowsBadRequest() {
        ProductRequestDto dto = buildRequest("Widget", "SKU-DUPE", "50.00", 5);

        when(productRepository.existsBySkuIgnoreCase("SKU-DUPE")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> productService.createProduct(dto, 1L));
        verify(productRepository, never()).save(any());
    }

    @Test
    void createProduct_whenSellerNotFound_thenThrowsResourceNotFound() {
        ProductRequestDto dto = buildRequest("Widget", "SKU-001", "50.00", 5);

        when(productRepository.existsBySkuIgnoreCase(anyString())).thenReturn(false);
        when(sellerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> productService.createProduct(dto, 99L));
    }

    @Test
    void createProduct_whenPriceIsZero_thenThrowsBadRequest() {
        ProductRequestDto dto = buildRequest("Widget", "SKU-001", "0.00", 5);

        assertThrows(BadRequestException.class, () -> productService.createProduct(dto, 1L));
        verify(productRepository, never()).save(any());
    }

    @Test
    void createProduct_whenPriceIsNegative_thenThrowsBadRequest() {
        ProductRequestDto dto = buildRequest("Widget", "SKU-001", "-1.00", 5);

        assertThrows(BadRequestException.class, () -> productService.createProduct(dto, 1L));
    }

    @Test
    void createProduct_whenQuantityIsNegative_thenThrowsBadRequest() {
        ProductRequestDto dto = buildRequest("Widget", "SKU-001", "50.00", -1);

        assertThrows(BadRequestException.class, () -> productService.createProduct(dto, 1L));
    }

    // -----------------------------------------------------------------------
    // updateProduct
    // -----------------------------------------------------------------------

    @Test
    void updateProduct_whenValid_thenReturnsUpdatedDto() {
        Product existing = buildProduct(10L);
        ProductRequestDto dto = buildRequest("Widget v2", "SKU-001", "75.00", 20);
        Stock existingStock = Stock.builder().product(existing).seller(seller).quantity(5).build();

        when(productRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(productRepository.existsBySkuIgnoreCaseAndIdNot("SKU-001", 10L)).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        when(stockRepository.findByProductId(10L)).thenReturn(Optional.of(existingStock));
        when(stockRepository.save(any(Stock.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductResponseDto result = productService.updateProduct(10L, dto, 1L);

        assertThat(result.getName()).isEqualTo("Widget v2");
        assertThat(result.getPrice()).isEqualByComparingTo("75.00");
    }

    @Test
    void updateProduct_whenProductNotFound_thenThrowsResourceNotFound() {
        ProductRequestDto dto = buildRequest("Widget", "SKU-001", "50.00", 5);

        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> productService.updateProduct(99L, dto, 1L));
    }

    @Test
    void updateProduct_whenSellerDoesNotOwnProduct_thenThrowsAccessDenied() {
        Product existing = buildProduct(10L); // owned by seller id=1
        ProductRequestDto dto = buildRequest("Widget", "SKU-001", "50.00", 5);

        when(productRepository.findById(10L)).thenReturn(Optional.of(existing));

        assertThrows(AccessDeniedException.class,
                () -> productService.updateProduct(10L, dto, 99L)); // different seller id
    }

    @Test
    void updateProduct_whenSkuTakenByOtherProduct_thenThrowsBadRequest() {
        Product existing = buildProduct(10L);
        ProductRequestDto dto = buildRequest("Widget", "SKU-TAKEN", "50.00", 5);

        when(productRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(productRepository.existsBySkuIgnoreCaseAndIdNot("SKU-TAKEN", 10L)).thenReturn(true);

        assertThrows(BadRequestException.class,
                () -> productService.updateProduct(10L, dto, 1L));
    }

    // -----------------------------------------------------------------------
    // deleteProduct
    // -----------------------------------------------------------------------

    @Test
    void deleteProduct_whenOwned_thenSetsProductInactive() {
        Product existing = buildProduct(10L);

        when(productRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        productService.deleteProduct(10L, 1L);

        assertThat(existing.getActive()).isFalse();
        verify(productRepository).save(existing);
    }

    @Test
    void deleteProduct_whenProductNotFound_thenThrowsResourceNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> productService.deleteProduct(99L, 1L));
    }

    // -----------------------------------------------------------------------
    // getProductById
    // -----------------------------------------------------------------------

    @Test
    void getProductById_whenActive_thenReturnsMappedDto() {
        Product product = buildProduct(5L);

        when(productRepository.findByIdAndActiveTrue(5L)).thenReturn(Optional.of(product));

        ProductResponseDto result = productService.getProductById(5L);

        assertThat(result.getId()).isEqualTo(5L);
        assertThat(result.getSku()).isEqualTo("SKU-001");
    }

    @Test
    void getProductById_whenNotFound_thenThrowsResourceNotFound() {
        when(productRepository.findByIdAndActiveTrue(anyLong())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> productService.getProductById(999L));
    }

    // -----------------------------------------------------------------------
    // getAllActiveProducts / getProductsBySeller
    // -----------------------------------------------------------------------

    @Test
    void getAllActiveProducts_thenReturnsMappedList() {
        Product p1 = buildProduct(1L);
        Product p2 = buildProduct(2L);

        when(productRepository.findByActiveTrue()).thenReturn(List.of(p1, p2));

        List<ProductResponseDto> result = productService.getAllActiveProducts();

        assertThat(result).hasSize(2);
    }

    @Test
    void getProductsBySeller_thenReturnsMappedList() {
        Product p = buildProduct(1L);

        when(productRepository.findBySellerId(1L)).thenReturn(List.of(p));

        List<ProductResponseDto> result = productService.getProductsBySeller(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSellerId()).isEqualTo(1L);
    }
}
