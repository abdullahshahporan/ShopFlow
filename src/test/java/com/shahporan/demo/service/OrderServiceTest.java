package com.shahporan.demo.service;

import com.shahporan.demo.dto.OrderItemRequestDto;
import com.shahporan.demo.dto.OrderRequestDto;
import com.shahporan.demo.dto.OrderResponseDto;
import com.shahporan.demo.dto.SellerOrderSummaryDto;
import com.shahporan.demo.entity.Order;
import com.shahporan.demo.entity.Product;
import com.shahporan.demo.entity.Seller;
import com.shahporan.demo.entity.Stock;
import com.shahporan.demo.entity.User;
import com.shahporan.demo.exception.BadRequestException;
import com.shahporan.demo.exception.ResourceNotFoundException;
import com.shahporan.demo.repository.CancelOrderRepository;
import com.shahporan.demo.repository.OrderRepository;
import com.shahporan.demo.repository.ProductRepository;
import com.shahporan.demo.repository.StockRepository;
import com.shahporan.demo.repository.UserRepository;
import com.shahporan.demo.strategy.PaymentMethod;
import com.shahporan.demo.strategy.PaymentResult;
import com.shahporan.demo.strategy.PaymentStrategy;
import com.shahporan.demo.strategy.PaymentStrategyResolver;
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
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StockRepository stockRepository;

    @Mock
    private CancelOrderRepository cancelOrderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentStrategyResolver paymentStrategyResolver;

    @Mock
    private PaymentStrategy paymentStrategy;

    @InjectMocks
    private OrderService orderService;

    private User buyer;
    private Seller seller;
    private Product product;
    private Stock stock;

    @BeforeEach
    void setUp() {
        buyer = User.builder().id(1L).name("Alice").email("alice@test.com")
                .passwordHash("hash").enabled(true).build();

        seller = Seller.builder().id(2L).name("Bob's Shop").email("bob@test.com")
                .passwordHash("hash").enabled(true).build();

        product = Product.builder().id(10L).seller(seller).name("Widget")
                .sku("SKU-01").price(new BigDecimal("100.00")).active(true).build();

        stock = Stock.builder().id(5L).product(product).seller(seller).quantity(50).build();
    }

    private OrderRequestDto buildOrderRequest(String paymentMethod, Long productId, int qty) {
        OrderItemRequestDto item = new OrderItemRequestDto();
        item.setProductId(productId);
        item.setQty(qty);

        OrderRequestDto dto = new OrderRequestDto();
        dto.setPaymentMethod(paymentMethod);
        dto.setItems(List.of(item));
        return dto;
    }

    // -----------------------------------------------------------------------
    // createOrder
    // -----------------------------------------------------------------------

    @Test
    void createOrder_whenValid_thenReturnsOrderResponse() {
        OrderRequestDto dto = buildOrderRequest("COD", 10L, 2);
        Order savedOrder = Order.builder()
                .id(100L)
                .buyer(buyer)
                .product(product)
                .qty(2)
                .unitPrice(product.getPrice())
                .status("PENDING")
                .paymentMethod("COD")
                .paymentStatus("PENDING")
                .total(new BigDecimal("200.00"))
                .build();

        PaymentResult paymentResult = PaymentResult.success(PaymentMethod.COD,
                new BigDecimal("200.00"), "PENDING", "COD order placed");

        when(userRepository.findById(1L)).thenReturn(Optional.of(buyer));
        when(productRepository.findAllById(any())).thenReturn(List.of(product));
        when(stockRepository.findByProductIdIn(anyCollection())).thenReturn(List.of(stock));
        when(paymentStrategyResolver.resolve("COD")).thenReturn(paymentStrategy);
        when(paymentStrategy.process(any(BigDecimal.class), any(Long.class))).thenReturn(paymentResult);
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(stockRepository.saveAll(any())).thenReturn(List.of(stock));

        OrderResponseDto result = orderService.createOrder(dto, 1L);

        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getStatus()).isEqualTo("PENDING");
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void createOrder_whenItemsEmpty_thenThrowsBadRequest() {
        OrderRequestDto dto = new OrderRequestDto();
        dto.setPaymentMethod("COD");
        dto.setItems(List.of());

        when(userRepository.findById(1L)).thenReturn(Optional.of(buyer));

        assertThrows(BadRequestException.class, () -> orderService.createOrder(dto, 1L));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_whenBuyerNotFound_thenThrowsResourceNotFound() {
        OrderRequestDto dto = buildOrderRequest("COD", 10L, 1);

        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> orderService.createOrder(dto, 99L));
    }

    @Test
    void createOrder_whenInsufficientStock_thenThrowsBadRequest() {
        stock.setQuantity(0); // zero stock for quantity=1 request
        OrderRequestDto dto = buildOrderRequest("COD", 10L, 1);

        when(userRepository.findById(1L)).thenReturn(Optional.of(buyer));
        when(productRepository.findAllById(any())).thenReturn(List.of(product));
        when(stockRepository.findByProductIdIn(anyCollection())).thenReturn(List.of(stock));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> orderService.createOrder(dto, 1L));
        assertThat(ex.getMessage()).contains("Insufficient stock");
    }

    @Test
    void createOrder_whenProductInactive_thenThrowsBadRequest() {
        product.setActive(false);
        OrderRequestDto dto = buildOrderRequest("COD", 10L, 1);

        when(userRepository.findById(1L)).thenReturn(Optional.of(buyer));
        when(productRepository.findAllById(any())).thenReturn(List.of(product));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> orderService.createOrder(dto, 1L));
        assertThat(ex.getMessage()).contains("inactive");
    }

    // -----------------------------------------------------------------------
    // cancelOrderByBuyer
    // -----------------------------------------------------------------------

    @Test
    void cancelOrder_whenStatusIsNotPending_thenThrowsBadRequest() {
        Order order = Order.builder()
                .id(1L).buyer(buyer).product(product).qty(1)
                .unitPrice(product.getPrice()).status("APPROVED")
                .total(new BigDecimal("100.00"))
                .paymentMethod("COD").paymentStatus("PENDING").build();

        when(orderRepository.findByIdAndBuyerId(1L, 1L)).thenReturn(Optional.of(order));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> orderService.cancelOrderByBuyer(1L, 1L, "Changed mind"));
        assertThat(ex.getMessage()).containsIgnoringCase("PENDING");
    }

    @Test
    void cancelOrder_whenAlreadyCancelled_thenThrowsBadRequest() {
        Order order = Order.builder()
                .id(1L).buyer(buyer).product(product).qty(1)
                .unitPrice(product.getPrice()).status("PENDING")
                .total(new BigDecimal("100.00"))
                .paymentMethod("COD").paymentStatus("PENDING").build();

        when(orderRepository.findByIdAndBuyerId(1L, 1L)).thenReturn(Optional.of(order));
        when(cancelOrderRepository.existsByOriginalOrderId(1L)).thenReturn(true);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> orderService.cancelOrderByBuyer(1L, 1L, "Changed mind"));
        assertThat(ex.getMessage()).containsIgnoringCase("already cancelled");
    }

    // -----------------------------------------------------------------------
    // updateOrderStatusBySeller
    // -----------------------------------------------------------------------

    @Test
    void updateOrderStatus_whenInvalidStatus_thenThrowsBadRequest() {
        assertThrows(BadRequestException.class,
                () -> orderService.updateOrderStatusBySeller(1L, 2L, "UNKNOWN_STATUS"));
    }

    @Test
    void updateOrderStatus_whenSellerDoesNotOwnOrder_thenThrowsAccessDenied() {
        when(orderRepository.existsByIdAndProductSellerId(1L, 99L)).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> orderService.updateOrderStatusBySeller(1L, 99L, "APPROVED"));
    }

    @Test
    void updateOrderStatus_whenValid_thenSavesNewStatus() {
        Order order = Order.builder()
                .id(1L).buyer(buyer).product(product).qty(1)
                .unitPrice(product.getPrice()).status("PENDING")
                .total(new BigDecimal("100.00"))
                .paymentMethod("COD").paymentStatus("PENDING").build();

        when(orderRepository.existsByIdAndProductSellerId(1L, 2L)).thenReturn(true);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        orderService.updateOrderStatusBySeller(1L, 2L, "approved");

        assertThat(order.getStatus()).isEqualTo("APPROVED");
        verify(orderRepository).save(order);
    }

    // -----------------------------------------------------------------------
    // getOrdersByBuyer
    // -----------------------------------------------------------------------

    @Test
    void getOrdersByBuyer_thenReturnsMappedList() {
        Order order = Order.builder()
                .id(1L).buyer(buyer).product(product).qty(2)
                .unitPrice(product.getPrice()).status("PENDING")
                .total(new BigDecimal("200.00"))
                .paymentMethod("COD").paymentStatus("PENDING").build();

        when(orderRepository.findByBuyerIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(order));

        List<OrderResponseDto> result = orderService.getOrdersByBuyer(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBuyerId()).isEqualTo(1L);
    }

    // -----------------------------------------------------------------------
    // getOrdersBySeller
    // -----------------------------------------------------------------------

    @Test
    void getOrdersBySeller_thenReturnsMappedSummaryList() {
        Order order = Order.builder()
                .id(1L).buyer(buyer).product(product).qty(3)
                .unitPrice(product.getPrice()).status("APPROVED")
                .total(new BigDecimal("300.00"))
                .paymentMethod("COD").paymentStatus("PENDING").build();

        when(orderRepository.findByProductSellerIdOrderByCreatedAtDesc(2L)).thenReturn(List.of(order));

        List<SellerOrderSummaryDto> result = orderService.getOrdersBySeller(2L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo("APPROVED");
    }
}
