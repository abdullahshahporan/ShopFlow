package com.shahporan.demo.controller;

import com.shahporan.demo.entity.User;
import com.shahporan.demo.repository.UserRepository;
import com.shahporan.demo.security.CustomUserDetails;
import com.shahporan.demo.security.RoleMappings;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class HomeController {

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

        User user = userRepository.findById(currentUser.getId()).orElse(null);
        if (user == null) {
            return "redirect:/login";
        }

        String roleLabel = switch (user.getRoleInt()) {
            case RoleMappings.ADMIN -> "Admin";
            case RoleMappings.SELLER -> "Seller";
            default -> "Buyer";
        };

        String dashboardUrl = switch (user.getRoleInt()) {
            case RoleMappings.ADMIN -> "/admin/users";
            case RoleMappings.SELLER -> "/seller/products";
            default -> "/buyer/orders";
        };

        model.addAttribute("profileUser", user);
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
