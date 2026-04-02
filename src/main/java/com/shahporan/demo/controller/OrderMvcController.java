package com.shahporan.demo.controller;

import com.shahporan.demo.dto.OrderItemRequestDto;
import com.shahporan.demo.dto.OrderRequestDto;
import com.shahporan.demo.security.CustomUserDetails;
import com.shahporan.demo.service.OrderService;
import com.shahporan.demo.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class OrderMvcController {

    private final OrderService orderService;
    private final ProductService productService;

    @GetMapping("/buyer/orders")
    public String orders(Model model, Authentication authentication) {
        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        List<com.shahporan.demo.dto.OrderResponseDto> orders = orderService.getOrdersByBuyer(user.getId());
        if (orders == null) {
            orders = new ArrayList<>();
        }
        BigDecimal totalSpent = orders.stream()
                .map(o -> o.getTotal() == null ? BigDecimal.ZERO : o.getTotal())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("orders", orders);
        model.addAttribute("totalSpent", totalSpent != null ? totalSpent : BigDecimal.ZERO);
        return "buyer/orders";
    }

    @GetMapping("/buyer/orders/new")
    public String newOrder(Model model) {
        if (!model.containsAttribute("orderRequestDto")) {
            model.addAttribute("orderRequestDto", new OrderRequestDto());
        }
        model.addAttribute("products", productService.getAllActiveProducts().stream()
                .filter(p -> p.getQuantity() != null && p.getQuantity() > 0)
                .toList());
        return "buyer/order-form";
    }

    @PostMapping("/buyer/orders")
    public String createOrder(@RequestParam("productId") java.util.List<Long> productIds,
                              @RequestParam("qty") java.util.List<Integer> qtys,
                              @RequestParam("paymentMethod") String paymentMethod,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes,
                              Model model) {
        OrderRequestDto dto = new OrderRequestDto();
        dto.setPaymentMethod(paymentMethod);
        dto.setItems(new ArrayList<>());

        for (int i = 0; i < productIds.size(); i++) {
            Integer qty = i < qtys.size() ? qtys.get(i) : 0;
            if (qty != null && qty > 0) {
                OrderItemRequestDto item = new OrderItemRequestDto();
                item.setProductId(productIds.get(i));
                item.setQty(qty);
                dto.getItems().add(item);
            }
        }

        try {
            CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
            orderService.createOrder(dto, user.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Order placed successfully.");
            return "redirect:/buyer/orders";
        } catch (RuntimeException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("orderRequestDto", dto);
            model.addAttribute("products", productService.getAllActiveProducts().stream()
                    .filter(p -> p.getQuantity() != null && p.getQuantity() > 0)
                    .toList());
            return "buyer/order-form";
        }
    }

    @GetMapping("/buyer/orders/{id}")
    public String orderDetail(@PathVariable Long id, Authentication authentication, Model model) {
        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        model.addAttribute("order", orderService.getOrderById(id, user.getId(), "ROLE_BUYER"));
        return "buyer/order-detail";
    }

    @PostMapping("/buyer/orders/{id}/cancel")
    public String cancelOrder(@PathVariable Long id,
                              @RequestParam(value = "reason", required = false) String reason,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        try {
            orderService.cancelOrderByBuyer(id, user.getId(), reason);
            redirectAttributes.addFlashAttribute("successMessage", "Order cancelled successfully.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/buyer/orders";
    }

    @GetMapping("/seller/orders")
    public String sellerOrders(Model model, Authentication authentication) {
        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        var orders = orderService.getOrdersBySeller(user.getId());
        long orderCount = orders.size();
        BigDecimal revenue = orders.stream()
                .map(o -> o.getTotalAmount() == null ? BigDecimal.ZERO : o.getTotalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("sellerOrders", orders);
        model.addAttribute("sellerOrderCount", orderCount);
        model.addAttribute("sellerRevenue", revenue);
        return "seller/orders";
    }

    @PostMapping("/seller/orders/{orderId}/status")
    public String updateSellerOrderStatus(@PathVariable Long orderId,
                                          @RequestParam("status") String status,
                                          Authentication authentication,
                                          RedirectAttributes redirectAttributes) {
        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        try {
            orderService.updateOrderStatusBySeller(orderId, user.getId(), status);
            redirectAttributes.addFlashAttribute("successMessage", "Order status updated successfully.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/seller/orders";
    }
}
