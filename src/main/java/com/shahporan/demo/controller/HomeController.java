package com.shahporan.demo.controller;

import com.shahporan.demo.dto.PasswordUpdateRequest;
import com.shahporan.demo.dto.ProfileUpdateRequest;
import com.shahporan.demo.dto.ProductResponseDto;
import com.shahporan.demo.entity.Admin;
import com.shahporan.demo.entity.Seller;
import com.shahporan.demo.entity.User;
import com.shahporan.demo.repository.AdminRepository;
import com.shahporan.demo.repository.SellerRepository;
import com.shahporan.demo.repository.UserRepository;
import com.shahporan.demo.security.CustomUserDetails;
import com.shahporan.demo.service.ProductService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final AdminRepository adminRepository;
    private final SellerRepository sellerRepository;
    private final UserRepository userRepository;
    private final ProductService productService;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/")
    public String home(Authentication authentication, HttpSession session, Model model) {
        boolean isSignedIn = authentication != null && authentication.getPrincipal() instanceof CustomUserDetails;
        String role = authentication != null
                ? authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).findFirst().orElse("")
                : "";
        boolean isBuyer = "ROLE_BUYER".equals(role);
        boolean isSellerOrAdmin = "ROLE_SELLER".equals(role) || "ROLE_ADMIN".equals(role);

        int cartCount = 0;
        Map<Long, Integer> cartQtyByProduct = new LinkedHashMap<>();
        Object rawCart = session.getAttribute("BUYER_CART");
        if (isBuyer && rawCart instanceof java.util.Map<?, ?> cartMap) {
            for (Map.Entry<?, ?> entry : cartMap.entrySet()) {
                if (entry.getKey() instanceof Long productId && entry.getValue() instanceof Integer qty) {
                    int safeQty = Math.max(0, qty);
                    cartQtyByProduct.put(productId, safeQty);
                    cartCount += safeQty;
                }
            }
        }

        List<ProductResponseDto> homeProducts = productService.getAllActiveProducts().stream()
                .sorted(Comparator.comparing(ProductResponseDto::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(12)
                .toList();

        model.addAttribute("isSignedIn", isSignedIn);
        model.addAttribute("isBuyer", isBuyer);
        model.addAttribute("isSellerOrAdmin", isSellerOrAdmin);
        model.addAttribute("cartCount", cartCount);
        model.addAttribute("cartQtyByProduct", cartQtyByProduct);
        model.addAttribute("homeProducts", homeProducts);
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
        if (!model.containsAttribute("profileUpdateRequest")) {
            model.addAttribute("profileUpdateRequest", ProfileUpdateRequest.builder().name(extractName(profileUser)).build());
        }
        if (!model.containsAttribute("passwordUpdateRequest")) {
            model.addAttribute("passwordUpdateRequest", new PasswordUpdateRequest());
        }

        return "profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@Valid @ModelAttribute("profileUpdateRequest") ProfileUpdateRequest profileUpdateRequest,
                                BindingResult bindingResult,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails currentUser)) {
            return "redirect:/login";
        }

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("profileErrorMessage", firstValidationMessage(bindingResult));
            redirectAttributes.addFlashAttribute("profileUpdateRequest", profileUpdateRequest);
            return "redirect:/profile";
        }

        String updatedName = profileUpdateRequest.getName().trim();
        String role = resolveRole(authentication);

        if ("ROLE_ADMIN".equals(role)) {
            Admin admin = adminRepository.findById(currentUser.getId()).orElse(null);
            if (admin == null) {
                return "redirect:/login";
            }
            admin.setName(updatedName);
            adminRepository.save(admin);
        } else if ("ROLE_SELLER".equals(role)) {
            Seller seller = sellerRepository.findById(currentUser.getId()).orElse(null);
            if (seller == null) {
                return "redirect:/login";
            }
            seller.setName(updatedName);
            sellerRepository.save(seller);
        } else {
            User user = userRepository.findById(currentUser.getId()).orElse(null);
            if (user == null) {
                return "redirect:/login";
            }
            user.setName(updatedName);
            userRepository.save(user);
        }

        redirectAttributes.addFlashAttribute("profileSuccessMessage", "Profile information updated successfully.");
        return "redirect:/profile";
    }

    @PostMapping("/profile/password")
    public String updatePassword(@Valid @ModelAttribute("passwordUpdateRequest") PasswordUpdateRequest passwordUpdateRequest,
                                 BindingResult bindingResult,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails currentUser)) {
            return "redirect:/login";
        }

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("passwordErrorMessage", firstValidationMessage(bindingResult));
            return "redirect:/profile";
        }

        if (!passwordUpdateRequest.getNewPassword().equals(passwordUpdateRequest.getConfirmPassword())) {
            redirectAttributes.addFlashAttribute("passwordErrorMessage", "New password and confirm password do not match.");
            return "redirect:/profile";
        }

        String role = resolveRole(authentication);
        boolean updated = false;

        if ("ROLE_ADMIN".equals(role)) {
            Admin admin = adminRepository.findById(currentUser.getId()).orElse(null);
            if (admin == null) {
                return "redirect:/login";
            }
            if (!passwordEncoder.matches(passwordUpdateRequest.getCurrentPassword(), admin.getPasswordHash())) {
                redirectAttributes.addFlashAttribute("passwordErrorMessage", "Current password is incorrect.");
                return "redirect:/profile";
            }
            admin.setPasswordHash(passwordEncoder.encode(passwordUpdateRequest.getNewPassword()));
            adminRepository.save(admin);
            updated = true;
        } else if ("ROLE_SELLER".equals(role)) {
            Seller seller = sellerRepository.findById(currentUser.getId()).orElse(null);
            if (seller == null) {
                return "redirect:/login";
            }
            if (!passwordEncoder.matches(passwordUpdateRequest.getCurrentPassword(), seller.getPasswordHash())) {
                redirectAttributes.addFlashAttribute("passwordErrorMessage", "Current password is incorrect.");
                return "redirect:/profile";
            }
            seller.setPasswordHash(passwordEncoder.encode(passwordUpdateRequest.getNewPassword()));
            sellerRepository.save(seller);
            updated = true;
        } else {
            User user = userRepository.findById(currentUser.getId()).orElse(null);
            if (user == null) {
                return "redirect:/login";
            }
            if (!passwordEncoder.matches(passwordUpdateRequest.getCurrentPassword(), user.getPasswordHash())) {
                redirectAttributes.addFlashAttribute("passwordErrorMessage", "Current password is incorrect.");
                return "redirect:/profile";
            }
            user.setPasswordHash(passwordEncoder.encode(passwordUpdateRequest.getNewPassword()));
            userRepository.save(user);
            updated = true;
        }

        if (updated) {
            redirectAttributes.addFlashAttribute("passwordSuccessMessage", "Password updated successfully. Use the new password for your next login.");
        }

        return "redirect:/profile";
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "access-denied";
    }

    private String resolveRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("ROLE_BUYER");
    }

    private String extractName(Object profileUser) {
        if (profileUser instanceof Admin admin) {
            return admin.getName();
        }
        if (profileUser instanceof Seller seller) {
            return seller.getName();
        }
        if (profileUser instanceof User user) {
            return user.getName();
        }
        return "";
    }

    private String firstValidationMessage(BindingResult bindingResult) {
        FieldError firstError = bindingResult.getFieldErrors().isEmpty() ? null : bindingResult.getFieldErrors().get(0);
        return firstError != null ? firstError.getDefaultMessage() : "Validation failed.";
    }
}
