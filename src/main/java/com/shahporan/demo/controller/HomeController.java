package com.shahporan.demo.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "home";
    }

    @GetMapping("/profile")
    public String profile(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return "redirect:/login";
        }

        if (authentication.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()))) {
            return "redirect:/admin/users";
        }
        if (authentication.getAuthorities().stream().anyMatch(a -> "ROLE_SELLER".equals(a.getAuthority()))) {
            return "redirect:/seller/products";
        }
        if (authentication.getAuthorities().stream().anyMatch(a -> "ROLE_BUYER".equals(a.getAuthority()))) {
            return "redirect:/buyer/orders";
        }

        return "redirect:/";
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "access-denied";
    }
}
