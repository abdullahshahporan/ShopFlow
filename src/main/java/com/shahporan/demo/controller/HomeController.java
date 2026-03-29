package com.shahporan.demo.controller;

import com.shahporan.demo.entity.Admin;
import com.shahporan.demo.entity.Seller;
import com.shahporan.demo.entity.User;
import com.shahporan.demo.repository.AdminRepository;
import com.shahporan.demo.repository.SellerRepository;
import com.shahporan.demo.repository.UserRepository;
import com.shahporan.demo.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final AdminRepository adminRepository;
    private final SellerRepository sellerRepository;
    private final UserRepository userRepository;

    @GetMapping("/")
    public String home(Authentication authentication, Model model) {
        boolean isSignedIn = authentication != null && authentication.getPrincipal() instanceof CustomUserDetails;
        model.addAttribute("isSignedIn", isSignedIn);
        return "home";
    }

    @GetMapping("/profile")
    public String profile(Authentication authentication, Model model) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails currentUser)) {
            return "redirect:/login";
        }

        String role = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("ROLE_BUYER");

        Object profileUser;
        String roleLabel;
        String dashboardUrl;

        if ("ROLE_ADMIN".equals(role)) {
            Admin admin = adminRepository.findById(currentUser.getId()).orElse(null);
            if (admin == null) {
                return "redirect:/login";
            }
            profileUser = admin;
            roleLabel = "Admin";
            dashboardUrl = "/admin/users";
        } else if ("ROLE_SELLER".equals(role)) {
            Seller seller = sellerRepository.findById(currentUser.getId()).orElse(null);
            if (seller == null) {
                return "redirect:/login";
            }
            profileUser = seller;
            roleLabel = "Seller";
            dashboardUrl = "/seller/products";
        } else {
            User user = userRepository.findById(currentUser.getId()).orElse(null);
            if (user == null) {
                return "redirect:/login";
            }
            profileUser = user;
            roleLabel = "Buyer";
            dashboardUrl = "/buyer/orders";
        }

        model.addAttribute("profileUser", profileUser);
        model.addAttribute("roleLabel", roleLabel);
        model.addAttribute("dashboardUrl", dashboardUrl);
        model.addAttribute("dashboardLabel", roleLabel + " Dashboard");

        return "profile";
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "access-denied";
    }
}
