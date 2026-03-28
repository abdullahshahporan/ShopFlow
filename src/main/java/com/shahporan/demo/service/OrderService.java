package com.shahporan.demo.service;

import com.shahporan.demo.dto.OrderItemRequestDto;
import com.shahporan.demo.dto.OrderItemResponseDto;
import com.shahporan.demo.dto.OrderRequestDto;
import com.shahporan.demo.dto.OrderResponseDto;
import com.shahporan.demo.entity.Order;
import com.shahporan.demo.entity.OrderItem;
import com.shahporan.demo.entity.Product;
import com.shahporan.demo.entity.StockMovement;
import com.shahporan.demo.entity.User;
import com.shahporan.demo.exception.BadRequestException;
import com.shahporan.demo.exception.ResourceNotFoundException;
import com.shahporan.demo.repository.OrderRepository;
import com.shahporan.demo.repository.ProductRepository;
import com.shahporan.demo.repository.StockMovementRepository;
import com.shahporan.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final StockMovementRepository stockMovementRepository;
    private final UserRepository userRepository;

    @Transactional
    public OrderResponseDto createOrder(OrderRequestDto dto, Long buyerId) {
        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new ResourceNotFoundException("Buyer not found with id: " + buyerId));

        Map<Long, Integer> requestedQtyByProduct = aggregateRequestedQuantities(dto.getItems());
        Map<Long, Product> productsById = loadAndValidateProducts(requestedQtyByProduct);

        Order order = Order.builder()
                .buyer(buyer)
                .status("PENDING")
                .total(BigDecimal.ZERO)
                .items(new ArrayList<>())
                .build();
        order = orderRepository.save(order);

        BigDecimal total = BigDecimal.ZERO;
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

            stockMovementRepository.save(StockMovement.builder()
                    .product(product)
                    .seller(product.getSeller())
                    .type(StockMovement.MovementType.OUT)
                    .qty(qty)
                    .note("Order #" + order.getId())
                    .build());

            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(qty)));
        }

        order.setTotal(total);
        order = orderRepository.save(order);

        return toResponse(order);
    }

    public List<OrderResponseDto> getOrdersByBuyer(Long buyerId) {
        return orderRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId).stream()
                .map(this::toResponse)
                .toList();
    }

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
        Map<Long, Product> productsById = new LinkedHashMap<>();

        for (Map.Entry<Long, Integer> entry : requestedQtyByProduct.entrySet()) {
            Long productId = entry.getKey();
            Integer requestedQty = entry.getValue();

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new BadRequestException("Product not found with id: " + productId));

            if (!Boolean.TRUE.equals(product.getActive())) {
                throw new BadRequestException("Product is inactive: " + product.getName());
            }

            if (product.getQuantity() == null || product.getQuantity() < requestedQty) {
                throw new BadRequestException("Insufficient stock for product: " + product.getName());
            }

            productsById.put(productId, product);
        }

        return productsById;
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
                .total(order.getTotal())
                .items(itemResponses)
                .createdAt(order.getCreatedAt())
                .build();
    }
}
