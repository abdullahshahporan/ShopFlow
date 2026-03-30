package com.shahporan.demo.service;

import com.shahporan.demo.dto.OrderItemRequestDto;
import com.shahporan.demo.dto.OrderItemResponseDto;
import com.shahporan.demo.dto.OrderRequestDto;
import com.shahporan.demo.dto.OrderResponseDto;
import com.shahporan.demo.dto.SellerOrderSummaryDto;
import com.shahporan.demo.entity.Order;
import com.shahporan.demo.entity.OrderItem;
import com.shahporan.demo.entity.Product;
import com.shahporan.demo.entity.StockMovement;
import com.shahporan.demo.entity.User;
import com.shahporan.demo.exception.BadRequestException;
import com.shahporan.demo.exception.ResourceNotFoundException;
import com.shahporan.demo.repository.OrderItemRepository;
import com.shahporan.demo.repository.OrderRepository;
import com.shahporan.demo.repository.ProductRepository;
import com.shahporan.demo.repository.StockMovementRepository;
import com.shahporan.demo.repository.UserRepository;
import com.shahporan.demo.strategy.PaymentResult;
import com.shahporan.demo.strategy.PaymentStrategy;
import com.shahporan.demo.strategy.PaymentStrategyResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final java.util.Set<String> ALLOWED_ORDER_STATUSES = java.util.Set.of(
            "PENDING",
            "APPROVED",
            "ON_THE_WAY",
            "DELIVERED"
    );

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final StockMovementRepository stockMovementRepository;
    private final UserRepository userRepository;
    private final PaymentStrategyResolver paymentStrategyResolver;

    @Transactional
    public OrderResponseDto createOrder(OrderRequestDto dto, Long buyerId) {
        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new ResourceNotFoundException("Buyer not found with id: " + buyerId));

        Map<Long, Integer> requestedQtyByProduct = aggregateRequestedQuantities(dto.getItems());
        Map<Long, Product> productsById = loadAndValidateProducts(requestedQtyByProduct);
        BigDecimal total = calculateTotal(requestedQtyByProduct, productsById);

        PaymentStrategy paymentStrategy = paymentStrategyResolver.resolve(dto.getPaymentMethod());
        PaymentResult paymentResult = paymentStrategy.process(total, buyerId);
        if (!paymentResult.isSuccess()) {
            throw new BadRequestException(paymentResult.getMessage());
        }

        Order order = Order.builder()
                .buyer(buyer)
                .status("PENDING")
            .paymentMethod(paymentResult.getPaymentMethod().name())
            .paymentStatus(paymentResult.getPaymentStatus())
            .total(total)
                .items(new ArrayList<>())
                .build();
        order = orderRepository.save(order);

        List<StockMovement> stockMovements = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : requestedQtyByProduct.entrySet()) {
            Product product = productsById.get(entry.getKey());
            int qty = entry.getValue();

            OrderItem item = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .qty(qty)
                    .unitPrice(product.getPrice())
                    .build();
            order.getItems().add(item);

            product.setQuantity(product.getQuantity() - qty);

                stockMovements.add(StockMovement.builder()
                    .product(product)
                    .seller(product.getSeller())
                    .type(StockMovement.MovementType.OUT)
                    .qty(qty)
                    .note("Order #" + order.getId())
                    .build());
        }

            productRepository.saveAll(productsById.values());
            stockMovementRepository.saveAll(stockMovements);
        order = orderRepository.save(order);

        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponseDto> getOrdersByBuyer(Long buyerId) {
        return orderRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId).stream()
                .map(this::toResponse)
                .toList();
    }

        @Transactional(readOnly = true)
        public List<SellerOrderSummaryDto> getOrdersBySeller(Long sellerId) {
        List<OrderItem> sellerItems = orderItemRepository.findAllBySellerIdWithOrderAndBuyer(sellerId);
        Map<Long, SellerOrderAccumulator> grouped = new LinkedHashMap<>();

        for (OrderItem item : sellerItems) {
            Order order = item.getOrder();
            SellerOrderAccumulator accumulator = grouped.computeIfAbsent(order.getId(), ignored ->
                new SellerOrderAccumulator(
                    order.getId(),
                    order.getBuyer() != null ? order.getBuyer().getName() : "Unknown Buyer",
                    order.getPaymentMethod(),
                    order.getPaymentStatus(),
                    order.getStatus(),
                    order.getCreatedAt()
                ));

            accumulator.totalUnits += item.getQty();
            accumulator.totalAmount = accumulator.totalAmount.add(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQty())));
            accumulator.uniqueProducts.add(item.getProduct().getId());
        }

        return grouped.values().stream()
            .map(SellerOrderAccumulator::toDto)
            .toList();
        }

    @Transactional
    public void updateOrderStatusBySeller(Long orderId, Long sellerId, String newStatus) {
        String normalized = normalizeStatus(newStatus);
        if (!ALLOWED_ORDER_STATUSES.contains(normalized)) {
            throw new BadRequestException("Invalid status. Allowed: PENDING, APPROVED, ON_THE_WAY, DELIVERED.");
        }

        boolean ownsOrder = orderItemRepository.existsByOrderIdAndProductSellerId(orderId, sellerId);
        if (!ownsOrder) {
            throw new AccessDeniedException("You are not allowed to update this order.");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        order.setStatus(normalized);
        orderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public OrderResponseDto getOrderById(Long orderId, Long buyerId, String role) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (!"ROLE_ADMIN".equals(role)) {
            if (!"ROLE_BUYER".equals(role)) {
                throw new AccessDeniedException("You are not allowed to access this order");
            }
            if (order.getBuyer() == null || !order.getBuyer().getId().equals(buyerId)) {
                throw new AccessDeniedException("You are not allowed to access this order");
            }
        }

        return toResponse(order);
    }

    private Map<Long, Integer> aggregateRequestedQuantities(List<OrderItemRequestDto> items) {
        if (items == null || items.isEmpty()) {
            throw new BadRequestException("Order must contain at least one item");
        }

        Map<Long, Integer> requestedQtyByProduct = new LinkedHashMap<>();
        for (OrderItemRequestDto item : items) {
            if (item.getProductId() == null || item.getQty() == null || item.getQty() < 1) {
                throw new BadRequestException("Each order item must include a productId and qty >= 1");
            }
            requestedQtyByProduct.merge(item.getProductId(), item.getQty(), Integer::sum);
        }
        return requestedQtyByProduct;
    }

    private Map<Long, Product> loadAndValidateProducts(Map<Long, Integer> requestedQtyByProduct) {
        List<Long> productIds = new ArrayList<>(requestedQtyByProduct.keySet());
        List<Product> products = productRepository.findAllById(productIds);

        if (products.size() != requestedQtyByProduct.size()) {
            throw new BadRequestException("One or more products were not found.");
        }

        Map<Long, Product> productsById = new LinkedHashMap<>();
        for (Product product : products) {
            productsById.put(product.getId(), product);
        }

        for (Map.Entry<Long, Integer> entry : requestedQtyByProduct.entrySet()) {
            Product product = productsById.get(entry.getKey());
            Integer requestedQty = entry.getValue();

            if (product == null) {
                throw new BadRequestException("Product not found with id: " + entry.getKey());
            }

            if (!Boolean.TRUE.equals(product.getActive())) {
                throw new BadRequestException("Product is inactive: " + product.getName());
            }

            if (product.getQuantity() == null || product.getQuantity() < requestedQty) {
                throw new BadRequestException("Insufficient stock for product: " + product.getName());
            }
        }

        return productsById;
    }

    private BigDecimal calculateTotal(Map<Long, Integer> requestedQtyByProduct, Map<Long, Product> productsById) {
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Long, Integer> entry : requestedQtyByProduct.entrySet()) {
            Product product = productsById.get(entry.getKey());
            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(entry.getValue())));
        }
        return total;
    }

    private OrderResponseDto toResponse(Order order) {
        List<OrderItemResponseDto> itemResponses = order.getItems().stream()
                .map(item -> OrderItemResponseDto.builder()
                        .productId(item.getProduct().getId())
                        .productName(item.getProduct().getName())
                        .qty(item.getQty())
                        .unitPrice(item.getUnitPrice())
                        .subtotal(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQty())))
                        .build())
                .toList();

        return OrderResponseDto.builder()
                .id(order.getId())
                .buyerId(order.getBuyer() != null ? order.getBuyer().getId() : null)
                .buyerName(order.getBuyer() != null ? order.getBuyer().getName() : null)
                .status(order.getStatus())
            .paymentMethod(order.getPaymentMethod())
            .paymentStatus(order.getPaymentStatus())
                .total(order.getTotal())
                .items(itemResponses)
                .createdAt(order.getCreatedAt())
                .build();
    }

    private static class SellerOrderAccumulator {
        private final Long orderId;
        private final String buyerName;
        private final String paymentMethod;
        private final String paymentStatus;
        private final String status;
        private final java.time.LocalDateTime createdAt;
        private int totalUnits = 0;
        private BigDecimal totalAmount = BigDecimal.ZERO;
        private final Set<Long> uniqueProducts = new HashSet<>();

        private SellerOrderAccumulator(Long orderId,
                                       String buyerName,
                                       String paymentMethod,
                                       String paymentStatus,
                                       String status,
                                       java.time.LocalDateTime createdAt) {
            this.orderId = orderId;
            this.buyerName = buyerName;
            this.paymentMethod = paymentMethod;
            this.paymentStatus = paymentStatus;
            this.status = status;
            this.createdAt = createdAt;
        }

        private SellerOrderSummaryDto toDto() {
            return SellerOrderSummaryDto.builder()
                    .orderId(orderId)
                    .buyerName(buyerName)
                    .paymentMethod(paymentMethod)
                    .paymentStatus(paymentStatus)
                    .status(status)
                    .totalUnits(totalUnits)
                    .totalAmount(totalAmount)
                    .createdAt(createdAt)
                    .build();
        }
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new BadRequestException("Order status is required.");
        }
        return status.trim().toUpperCase(Locale.ROOT);
    }
}
