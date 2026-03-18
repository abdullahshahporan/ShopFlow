package com.shahporan.demo.controller;

import com.shahporan.demo.security.CustomUserDetails;
import com.shahporan.demo.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AdminMvcController {

    private final AdminService adminService;

    @GetMapping("/admin/users")
    public String users(Model model, Authentication authentication) {
        CustomUserDetails current = (CustomUserDetails) authentication.getPrincipal();
        model.addAttribute("users", adminService.getAllUsers());
        model.addAttribute("currentAdminId", current.getId());
        return "admin/users";
    }

    @PostMapping("/admin/users/{id}/role")
    public String changeRole(@PathVariable Long id,
                             @RequestParam("roleInt") Integer roleInt,
                             RedirectAttributes redirectAttributes) {
        try {
            adminService.changeRole(id, roleInt);
            redirectAttributes.addFlashAttribute("successMessage", "User role updated successfully.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/users";
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
