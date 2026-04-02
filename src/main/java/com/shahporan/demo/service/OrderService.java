package com.shahporan.demo.service;

import com.shahporan.demo.dto.OrderItemRequestDto;
import com.shahporan.demo.dto.OrderItemResponseDto;
import com.shahporan.demo.dto.OrderRequestDto;
import com.shahporan.demo.dto.OrderResponseDto;
import com.shahporan.demo.dto.SellerOrderSummaryDto;
import com.shahporan.demo.entity.CancelOrder;
import com.shahporan.demo.entity.Order;
import com.shahporan.demo.entity.Product;
import com.shahporan.demo.entity.Stock;
import com.shahporan.demo.entity.User;
import com.shahporan.demo.exception.BadRequestException;
import com.shahporan.demo.exception.ResourceNotFoundException;
import com.shahporan.demo.repository.CancelOrderRepository;
import com.shahporan.demo.repository.OrderRepository;
import com.shahporan.demo.repository.ProductRepository;
import com.shahporan.demo.repository.StockRepository;
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
import java.util.Locale;
import java.util.stream.Collectors;

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
    private final ProductRepository productRepository;
    private final StockRepository stockRepository;
    private final CancelOrderRepository cancelOrderRepository;
    private final UserRepository userRepository;
    private final PaymentStrategyResolver paymentStrategyResolver;

    @Transactional
    public OrderResponseDto createOrder(OrderRequestDto dto, Long buyerId) {
        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new ResourceNotFoundException("Buyer not found with id: " + buyerId));

        Map<Long, Integer> requestedQtyByProduct = aggregateRequestedQuantities(dto.getItems());
        Map<Long, Product> productsById = loadAndValidateProducts(requestedQtyByProduct);
        Map<Long, Stock> stockByProductId = loadAndValidateStock(requestedQtyByProduct, productsById);
        BigDecimal total = calculateTotal(requestedQtyByProduct, productsById);

        PaymentStrategy paymentStrategy = paymentStrategyResolver.resolve(dto.getPaymentMethod());
        PaymentResult paymentResult = paymentStrategy.process(total, buyerId);
        if (!paymentResult.isSuccess()) {
            throw new BadRequestException(paymentResult.getMessage());
        }

        List<Stock> touchedStocks = new ArrayList<>();
        List<Order> createdOrders = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : requestedQtyByProduct.entrySet()) {
            Product product = productsById.get(entry.getKey());
            int qty = entry.getValue();
            Stock stock = stockByProductId.get(entry.getKey());

            int remainingQty = stock.getQuantity() - qty;
            stock.setQuantity(remainingQty);
            product.setQuantity(remainingQty);
            touchedStocks.add(stock);

            Order order = Order.builder()
                    .buyer(buyer)
                    .product(product)
                    .qty(qty)
                    .unitPrice(product.getPrice())
                    .status("PENDING")
                    .paymentMethod(paymentResult.getPaymentMethod().name())
                    .paymentStatus(paymentResult.getPaymentStatus())
                    .total(product.getPrice().multiply(BigDecimal.valueOf(qty)))
                    .build();
            createdOrders.add(orderRepository.save(order));
        }

        stockRepository.saveAll(touchedStocks);
        productRepository.saveAll(productsById.values());

        return toResponse(createdOrders.get(0));
    }

    @Transactional
    public void cancelOrderByBuyer(Long orderId, Long buyerId, String reason) {
        Order order = orderRepository.findByIdAndBuyerId(orderId, buyerId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (!"PENDING".equals(normalizeStatus(order.getStatus()))) {
            throw new BadRequestException("Order can be cancelled only before seller approval (PENDING status).");
        }

        if (cancelOrderRepository.existsByOriginalOrderId(orderId)) {
            throw new BadRequestException("This order is already cancelled.");
        }

        Product product = order.getProduct();
        Stock stock = stockRepository.findByProductId(product.getId())
            .orElseGet(() -> stockRepository.save(Stock.builder()
                .product(product)
                .seller(product.getSeller())
                .quantity(product.getQuantity() == null ? 0 : product.getQuantity())
                .build()));
        int restoredQty = (stock.getQuantity() == null ? 0 : stock.getQuantity()) + order.getQty();
        stock.setQuantity(restoredQty);
        product.setQuantity(restoredQty);
        stockRepository.save(stock);
        productRepository.save(product);

        CancelOrder cancelOrder = CancelOrder.builder()
                .originalOrderId(order.getId())
                .buyer(order.getBuyer())
                .status("CANCELLED")
                .reason(trimReason(reason))
                .total(order.getTotal())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .itemsSnapshot(buildItemsSnapshot(order))
                .orderCreatedAt(order.getCreatedAt())
                .build();
        cancelOrderRepository.save(cancelOrder);

        orderRepository.delete(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponseDto> getOrdersByBuyer(Long buyerId) {
        return orderRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId).stream()
                .map(this::toResponse)
                .toList();
    }

        @Transactional(readOnly = true)
        public List<SellerOrderSummaryDto> getOrdersBySeller(Long sellerId) {
        return orderRepository.findByProductSellerIdOrderByCreatedAtDesc(sellerId).stream()
            .map(order -> SellerOrderSummaryDto.builder()
                .orderId(order.getId())
                .buyerName(order.getBuyer() != null ? order.getBuyer().getName() : "Unknown Buyer")
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .status(order.getStatus())
                .totalUnits(order.getQty())
                .totalAmount(order.getTotal())
                .createdAt(order.getCreatedAt())
                .build())
            .toList();
        }

    @Transactional
    public void updateOrderStatusBySeller(Long orderId, Long sellerId, String newStatus) {
        String normalized = normalizeStatus(newStatus);
        if (!ALLOWED_ORDER_STATUSES.contains(normalized)) {
            throw new BadRequestException("Invalid status. Allowed: PENDING, APPROVED, ON_THE_WAY, DELIVERED.");
        }

        boolean ownsOrder = orderRepository.existsByIdAndProductSellerId(orderId, sellerId);
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

    private Map<Long, Stock> loadAndValidateStock(Map<Long, Integer> requestedQtyByProduct,
                                                  Map<Long, Product> productsById) {
        List<Stock> existingStocks = stockRepository.findByProductIdIn(requestedQtyByProduct.keySet());
        Map<Long, Stock> stockByProductId = existingStocks.stream()
                .collect(Collectors.toMap(stock -> stock.getProduct().getId(), stock -> stock));

        for (Map.Entry<Long, Integer> entry : requestedQtyByProduct.entrySet()) {
            Long productId = entry.getKey();
            Product product = productsById.get(productId);

            Stock stock = stockByProductId.get(productId);
            if (stock == null) {
                stock = stockRepository.save(Stock.builder()
                        .product(product)
                        .seller(product.getSeller())
                        .quantity(product.getQuantity() == null ? 0 : product.getQuantity())
                        .build());
                stockByProductId.put(productId, stock);
            }

            int availableQty = stock.getQuantity() == null ? 0 : stock.getQuantity();
            if (availableQty < entry.getValue()) {
                throw new BadRequestException("Insufficient stock for product: " + product.getName());
            }
        }

        return stockByProductId;
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
        Product product = order.getProduct();
        OrderItemResponseDto itemDto = OrderItemResponseDto.builder()
            .productId(product != null ? product.getId() : null)
            .productName(product != null ? product.getName() : "Unknown Product")
            .imageUrl(product != null ? product.getImageUrl() : null)
            .qty(order.getQty())
            .unitPrice(order.getUnitPrice())
            .subtotal(order.getTotal() != null ? order.getTotal() : BigDecimal.ZERO)
            .build();

        return OrderResponseDto.builder()
                .id(order.getId())
                .buyerId(order.getBuyer() != null ? order.getBuyer().getId() : null)
                .buyerName(order.getBuyer() != null ? order.getBuyer().getName() : null)
                .status(order.getStatus() != null ? order.getStatus() : "PENDING")
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .total(order.getTotal() != null ? order.getTotal() : BigDecimal.ZERO)
                .items(List.of(itemDto))
                .createdAt(order.getCreatedAt())
                .build();
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new BadRequestException("Order status is required.");
        }
        return status.trim().toUpperCase(Locale.ROOT);
    }

    private String trimReason(String reason) {
        if (reason == null) {
            return null;
        }
        String trimmed = reason.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.length() > 500 ? trimmed.substring(0, 500) : trimmed;
    }

    private String buildItemsSnapshot(Order order) {
        return order.getProduct().getName() + " x " + order.getQty();
    }
}
