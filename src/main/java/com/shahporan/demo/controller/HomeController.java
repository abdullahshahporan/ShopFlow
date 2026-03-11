package com.shahporan.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Day 1 — Simple controller to render placeholder pages.
 */
@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "home";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    @GetMapping("/products")
    public String products() {
        return "products";
    }

    @GetMapping("/seller/products")
    public String sellerProducts() {
        return "seller/products";
    }

    @GetMapping("/buyer/orders")
    public String buyerOrders() {
        return "buyer/orders";
    }

    @GetMapping("/admin/users")
    public String adminUsers() {
        return "admin/users";
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "access-denied";
    }
}
