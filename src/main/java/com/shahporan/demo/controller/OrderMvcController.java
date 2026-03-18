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

import java.util.ArrayList;

@Controller
@RequiredArgsConstructor
public class OrderMvcController {

    private final OrderService orderService;
    private final ProductService productService;

    @GetMapping("/buyer/orders")
    public String orders(Model model, Authentication authentication) {
        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        model.addAttribute("orders", orderService.getOrdersByBuyer(user.getId()));
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
                              Authentication authentication,
                              RedirectAttributes redirectAttributes,
                              Model model) {
        OrderRequestDto dto = new OrderRequestDto();
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
}
