package com.shahporan.demo.controller;

import com.shahporan.demo.dto.OrderItemRequestDto;
import com.shahporan.demo.dto.OrderRequestDto;
import com.shahporan.demo.dto.ProductResponseDto;
import com.shahporan.demo.security.CustomUserDetails;
import com.shahporan.demo.service.OrderService;
import com.shahporan.demo.service.ProductService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class CartMvcController {

    private static final String CART_SESSION_KEY = "BUYER_CART";

    private final ProductService productService;
    private final OrderService orderService;

    @GetMapping("/buyer/cart")
    public String cart(Model model, HttpSession session) {
        Map<Long, Integer> cart = getCart(session);

        List<CartLineView> lines = new ArrayList<>();
        BigDecimal grandTotal = BigDecimal.ZERO;
        List<Long> invalidProductIds = new ArrayList<>();

        for (Map.Entry<Long, Integer> entry : cart.entrySet()) {
            try {
                ProductResponseDto product = productService.getProductById(entry.getKey());
                int qty = Math.max(1, entry.getValue());
                int available = product.getQuantity() == null ? 0 : product.getQuantity();
                if (available < 1) {
                    // Remove out-of-stock products from session cart.
                    invalidProductIds.add(entry.getKey());
                    continue;
                }
                int finalQty = Math.min(qty, available);
                BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(finalQty));
                grandTotal = grandTotal.add(subtotal);
                lines.add(new CartLineView(product, finalQty, subtotal));
            } catch (RuntimeException ex) {
                // Remove products that no longer exist or are inactive from session cart.
                invalidProductIds.add(entry.getKey());
            }
        }

        invalidProductIds.forEach(cart::remove);

        session.setAttribute(CART_SESSION_KEY, cart);
        model.addAttribute("cartLines", lines);
        model.addAttribute("cartTotal", grandTotal);
        model.addAttribute("cartItemCount", lines.stream().mapToInt(CartLineView::getQty).sum());
        return "buyer/cart";
    }

    @PostMapping("/buyer/cart/add")
    public String addToCart(@RequestParam("productId") Long productId,
                            @RequestParam(value = "qty", defaultValue = "1") Integer qty,
                            @RequestParam(value = "returnTo", required = false) String returnTo,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        CartUpdateResult result = applyAddToCart(session, productId, qty);
        if (!result.success()) {
            redirectAttributes.addFlashAttribute("errorMessage", result.message());
            return "redirect:" + resolveReturnTarget(returnTo, "/");
        }
        redirectAttributes.addFlashAttribute("successMessage", result.message());
        return "redirect:" + resolveReturnTarget(returnTo, "/");
    }

    @PostMapping("/buyer/cart/update")
    public String updateQty(@RequestParam("productId") Long productId,
                            @RequestParam("action") String action,
                            @RequestParam(value = "returnTo", required = false) String returnTo,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        CartUpdateResult result = applyUpdateQty(session, productId, action);
        if (!result.success()) {
            redirectAttributes.addFlashAttribute("errorMessage", result.message());
            return "redirect:" + resolveReturnTarget(returnTo, "/buyer/cart");
        }
        redirectAttributes.addFlashAttribute("successMessage", result.message());
        return "redirect:" + resolveReturnTarget(returnTo, "/buyer/cart");
    }

    @PostMapping("/buyer/cart/api/add")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> addToCartApi(@RequestParam("productId") Long productId,
                                                             @RequestParam(value = "qty", defaultValue = "1") Integer qty,
                                                             HttpSession session) {
        CartUpdateResult result = applyAddToCart(session, productId, qty);
        return ResponseEntity.status(result.success() ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                .body(toApiResponse(result));
    }

    @PostMapping("/buyer/cart/api/update")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateQtyApi(@RequestParam("productId") Long productId,
                                                             @RequestParam("action") String action,
                                                             HttpSession session) {
        CartUpdateResult result = applyUpdateQty(session, productId, action);
        return ResponseEntity.status(result.success() ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                .body(toApiResponse(result));
    }

    @PostMapping("/buyer/cart/remove")
    public String remove(@RequestParam("productId") Long productId,
                         @RequestParam(value = "returnTo", required = false) String returnTo,
                         HttpSession session,
                         RedirectAttributes redirectAttributes) {
        Map<Long, Integer> cart = getCart(session);
        cart.remove(productId);
        session.setAttribute(CART_SESSION_KEY, cart);
        redirectAttributes.addFlashAttribute("successMessage", "Item removed from cart.");
        return "redirect:" + resolveReturnTarget(returnTo, "/buyer/cart");
    }

    @PostMapping("/buyer/cart/checkout")
    public String checkout(Authentication authentication,
                           @RequestParam("paymentMethod") String paymentMethod,
                           HttpSession session,
                           RedirectAttributes redirectAttributes) {
        Map<Long, Integer> cart = getCart(session);
        if (cart.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Your cart is empty.");
            return "redirect:/buyer/cart";
        }

        OrderRequestDto dto = new OrderRequestDto();
        List<OrderItemRequestDto> items = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : cart.entrySet()) {
            OrderItemRequestDto item = new OrderItemRequestDto();
            item.setProductId(entry.getKey());
            item.setQty(entry.getValue());
            items.add(item);
        }
        dto.setPaymentMethod(paymentMethod);
        dto.setItems(items);

        try {
            CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
            orderService.createOrder(dto, user.getId());
            session.removeAttribute(CART_SESSION_KEY);
            redirectAttributes.addFlashAttribute("successMessage", "Order placed successfully.");
            return "redirect:/buyer/orders";
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/buyer/cart";
        }
    }

    private Map<Long, Integer> getCart(HttpSession session) {
        Object raw = session.getAttribute(CART_SESSION_KEY);
        if (raw instanceof Map<?, ?> map) {
            Map<Long, Integer> casted = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() instanceof Long id && entry.getValue() instanceof Integer qty) {
                    casted.put(id, qty);
                }
            }
            return casted;
        }
        return new LinkedHashMap<>();
    }

    private CartUpdateResult applyAddToCart(HttpSession session, Long productId, Integer qty) {
        ProductResponseDto product = productService.getProductById(productId);
        int available = product.getQuantity() == null ? 0 : product.getQuantity();
        if (available < 1) {
            return failure(session, productId, "This product is out of stock.");
        }

        int requestedQty = qty == null ? 1 : qty;
        int safeQty = Math.max(1, requestedQty);

        Map<Long, Integer> cart = getCart(session);
        int current = cart.getOrDefault(productId, 0);
        int nextQty = Math.min(current + safeQty, available);
        cart.put(productId, nextQty);
        session.setAttribute(CART_SESSION_KEY, cart);

        return success(cart, productId, nextQty, "Added to cart: " + product.getName());
    }

    private CartUpdateResult applyUpdateQty(HttpSession session, Long productId, String action) {
        Map<Long, Integer> cart = getCart(session);
        Integer current = cart.get(productId);
        if (current == null) {
            return failure(session, productId, "Item not found in cart.");
        }

        ProductResponseDto product = productService.getProductById(productId);
        int available = product.getQuantity() == null ? 0 : product.getQuantity();
        int next = current;

        if ("inc".equalsIgnoreCase(action)) {
            next = Math.min(current + 1, Math.max(available, 1));
        } else if ("dec".equalsIgnoreCase(action)) {
            next = current - 1;
        }

        if (next <= 0) {
            cart.remove(productId);
            session.setAttribute(CART_SESSION_KEY, cart);
            return success(cart, productId, 0, "Item removed from cart.");
        }

        cart.put(productId, next);
        session.setAttribute(CART_SESSION_KEY, cart);
        return success(cart, productId, next, "Cart updated.");
    }

    private CartUpdateResult success(Map<Long, Integer> cart, Long productId, int productQty, String message) {
        return new CartUpdateResult(true, message, productId, productQty, cartItemCount(cart));
    }

    private CartUpdateResult failure(HttpSession session, Long productId, String message) {
        Map<Long, Integer> cart = getCart(session);
        return new CartUpdateResult(false, message, productId, cart.getOrDefault(productId, 0), cartItemCount(cart));
    }

    private Map<String, Object> toApiResponse(CartUpdateResult result) {
        Map<String, Object> body = new HashMap<>();
        body.put("success", result.success());
        body.put("message", result.message());
        body.put("productId", result.productId());
        body.put("productQty", result.productQty());
        body.put("cartCount", result.cartCount());
        return body;
    }

    private int cartItemCount(Map<Long, Integer> cart) {
        return cart.values().stream().mapToInt(qty -> Math.max(qty, 0)).sum();
    }

    private static class CartLineView {
        private final ProductResponseDto product;
        private final int qty;
        private final BigDecimal subtotal;

        private CartLineView(ProductResponseDto product, int qty, BigDecimal subtotal) {
            this.product = product;
            this.qty = qty;
            this.subtotal = subtotal;
        }

        public ProductResponseDto getProduct() {
            return product;
        }

        public int getQty() {
            return qty;
        }

        public BigDecimal getSubtotal() {
            return subtotal;
        }
    }

    private record CartUpdateResult(boolean success, String message, Long productId, int productQty, int cartCount) {
    }

    private String resolveReturnTarget(String returnTo, String defaultTarget) {
        if (returnTo == null || returnTo.isBlank()) {
            return defaultTarget;
        }
        if ("home".equalsIgnoreCase(returnTo)) {
            return "/";
        }
        if ("cart".equalsIgnoreCase(returnTo)) {
            return "/buyer/cart";
        }
        return defaultTarget;
    }
}
