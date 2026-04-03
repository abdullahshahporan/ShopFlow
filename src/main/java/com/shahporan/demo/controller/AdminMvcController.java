package com.shahporan.demo.controller;

import com.shahporan.demo.dto.UserResponseDto;
import com.shahporan.demo.security.CustomUserDetails;
import com.shahporan.demo.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class AdminMvcController {

    private final AdminService adminService;

    @GetMapping("/admin/users")
    public String users(Model model, Authentication authentication) {
        CustomUserDetails current = (CustomUserDetails) authentication.getPrincipal();
        List<UserResponseDto> users = adminService.getAllUsers();

        long buyerCount = adminService.countBuyers();
        long sellerCount = adminService.countSellers();
        long adminCount = adminService.countAdmins();
        long pendingSellerCount = adminService.countPendingSellers();

        model.addAttribute("users", users);
        model.addAttribute("buyerCount", buyerCount);
        model.addAttribute("sellerCount", sellerCount);
        model.addAttribute("adminCount", adminCount);
        model.addAttribute("pendingSellerCount", pendingSellerCount);
        model.addAttribute("currentAdminId", current.getId());
        return "admin/users";
    }

    @GetMapping("/admin/sellers")
    public String sellers(Model model) {
        List<UserResponseDto> sellers = adminService.getAllSellers();
        long pendingSellerCount = adminService.countPendingSellers();
        long totalSellerCount = adminService.countSellers();
        model.addAttribute("sellers", sellers);
        model.addAttribute("pendingSellerCount", pendingSellerCount);
        model.addAttribute("totalSellerCount", totalSellerCount);
        return "admin/sellers";
    }

    @PostMapping("/admin/sellers/{id}/toggle")
    public String toggleSeller(@PathVariable Long id,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        try {
            CustomUserDetails current = (CustomUserDetails) authentication.getPrincipal();
            adminService.toggleUser(id, current.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Seller status updated successfully.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/sellers";
    }

    @PostMapping("/admin/users/{id}/toggle")
    public String toggleUser(@PathVariable Long id,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        try {
            CustomUserDetails current = (CustomUserDetails) authentication.getPrincipal();
            adminService.toggleUser(id, current.getId());
            redirectAttributes.addFlashAttribute("successMessage", "User status updated successfully.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/users";
    }
}
