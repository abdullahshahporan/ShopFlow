package com.shahporan.demo.controller;

import com.shahporan.demo.dto.ProductRequestDto;
import com.shahporan.demo.dto.ProductResponseDto;
import com.shahporan.demo.security.CustomUserDetails;
import com.shahporan.demo.service.ProductService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class ProductMvcController {

    private final ProductService productService;

    @GetMapping("/products")
    public String products(Model model, Authentication authentication, HttpSession session) {
        List<ProductResponseDto> products = productService.getAllActiveProducts();
        model.addAttribute("products", products != null ? products : new ArrayList<>());

        boolean isBuyer = authentication != null && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).anyMatch("ROLE_BUYER"::equals);
        int cartCount = 0;
        if (isBuyer) {
            Object rawCart = session.getAttribute("BUYER_CART");
            if (rawCart instanceof Map<?, ?> cartMap) {
                for (Object val : cartMap.values()) {
                    if (val instanceof Integer qty) cartCount += Math.max(0, qty);
                }
            }
        }
        model.addAttribute("cartCount", cartCount);
        return "products";
    }

    @GetMapping("/seller/products")
    public String sellerProducts(Model model, Authentication authentication) {
        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        List<ProductResponseDto> products = productService.getProductsBySeller(user.getId());
        long activeProductCount = products.stream().filter(ProductResponseDto::isActive).count();
        long outOfStockCount = products.stream().filter(p -> p.getQuantity() != null && p.getQuantity() == 0).count();

        model.addAttribute("products", products);
        model.addAttribute("activeProductCount", activeProductCount);
        model.addAttribute("outOfStockCount", outOfStockCount);
        return "seller/products";
    }

    @GetMapping("/seller/products/new")
    public String newProduct(Model model) {
        if (!model.containsAttribute("productRequestDto")) {
            model.addAttribute("productRequestDto", new ProductRequestDto());
        }
        model.addAttribute("isEdit", false);
        return "seller/product-form";
    }

    @PostMapping("/seller/products")
    public String createProduct(@Valid @ModelAttribute("productRequestDto") ProductRequestDto dto,
                                BindingResult bindingResult,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("isEdit", false);
            return "seller/product-form";
        }

        try {
            CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
            productService.createProduct(dto, user.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Product created successfully.");
            return "redirect:/seller/products";
        } catch (RuntimeException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("isEdit", false);
            return "seller/product-form";
        }
    }

    @GetMapping("/seller/products/{id}/edit")
    public String editProduct(@PathVariable Long id, Model model, Authentication authentication) {
        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        var product = productService.getProductsBySeller(user.getId()).stream()
                .filter(p -> p.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("You do not own this product"));

        ProductRequestDto dto = new ProductRequestDto();
        dto.setName(product.getName());
        dto.setSku(product.getSku());
        dto.setPrice(product.getPrice());
        dto.setQuantity(product.getQuantity());
        dto.setActive(product.isActive());

        model.addAttribute("productRequestDto", dto);
        model.addAttribute("productId", id);
        model.addAttribute("isEdit", true);
        return "seller/product-form";
    }

    @PostMapping("/seller/products/{id}")
    public String updateProduct(@PathVariable Long id,
                                @Valid @ModelAttribute("productRequestDto") ProductRequestDto dto,
                                BindingResult bindingResult,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("productId", id);
            model.addAttribute("isEdit", true);
            return "seller/product-form";
        }

        try {
            CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
            productService.updateProduct(id, dto, user.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Product updated successfully.");
            return "redirect:/seller/products";
        } catch (RuntimeException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("productId", id);
            model.addAttribute("isEdit", true);
            return "seller/product-form";
        }
    }

    @PostMapping("/seller/products/{id}/delete")
    public String deleteProduct(@PathVariable Long id,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        try {
            CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
            productService.deleteProduct(id, user.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Product deleted successfully.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/seller/products";
    }
}
