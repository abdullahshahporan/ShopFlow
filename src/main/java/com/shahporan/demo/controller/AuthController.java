package com.shahporan.demo.controller;

import com.shahporan.demo.dto.RegisterRequest;
import com.shahporan.demo.entity.Seller;
import com.shahporan.demo.entity.User;
import com.shahporan.demo.repository.AdminRepository;
import com.shahporan.demo.repository.SellerRepository;
import com.shahporan.demo.repository.UserRepository;
import com.shahporan.demo.security.RoleMappings;
import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final UserRepository userRepository;
    private final SellerRepository sellerRepository;
    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository,
                          SellerRepository sellerRepository,
                          AdminRepository adminRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.sellerRepository = sellerRepository;
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String registerDefault() {
        return "redirect:/register/buyer";
    }

    @GetMapping("/register/buyer")
    public String registerBuyerPage(Model model) {
        prepareRegisterPage(model, "BUYER", "Create Buyer Account",
                "Buyer accounts are active immediately after registration.");
        return "register";
    }

    @GetMapping("/register/seller")
    public String registerSellerPage(Model model) {
        prepareRegisterPage(model, "SELLER", "Create Seller Account",
                "Seller accounts require admin approval before login.");
        return "register";
    }

    @PostMapping("/register/buyer")
    public String registerBuyer(@Valid @ModelAttribute("registerRequest") RegisterRequest registerRequest,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (hasRegistrationErrors(registerRequest, bindingResult)) {
            prepareRegisterPage(model, "BUYER", "Create Buyer Account",
                    "Buyer accounts are active immediately after registration.");
            return "register";
        }

        saveBuyer(registerRequest);
        redirectAttributes.addFlashAttribute("successMessage", "Buyer account created successfully. You can now sign in.");
        return "redirect:/login?registeredBuyer";
    }

    @PostMapping("/register/seller")
    public String registerSeller(@Valid @ModelAttribute("registerRequest") RegisterRequest registerRequest,
                                 BindingResult bindingResult,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        if (hasRegistrationErrors(registerRequest, bindingResult)) {
            prepareRegisterPage(model, "SELLER", "Create Seller Account",
                    "Seller accounts require admin approval before login.");
            return "register";
        }

        saveSeller(registerRequest);
        redirectAttributes.addFlashAttribute("successMessage", "Seller registration request submitted. Please wait for admin approval.");
        return "redirect:/login?sellerPending";
    }

    private boolean hasRegistrationErrors(RegisterRequest registerRequest, BindingResult bindingResult) {
        String normalizedEmail = registerRequest.getEmail() == null ? ""
                : registerRequest.getEmail().trim().toLowerCase();

        if (!registerRequest.getPassword().equals(registerRequest.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "password.mismatch", "Passwords do not match");
        }

        if (emailExistsInAnyAccountTable(normalizedEmail)) {
            bindingResult.rejectValue("email", "email.duplicate", "Email is already registered");
        }

        return bindingResult.hasErrors();
    }

    private boolean emailExistsInAnyAccountTable(String normalizedEmail) {
        return userRepository.existsByEmailIgnoreCase(normalizedEmail)
                || sellerRepository.existsByEmailIgnoreCase(normalizedEmail)
                || adminRepository.existsByEmailIgnoreCase(normalizedEmail);
    }

    private void saveBuyer(RegisterRequest registerRequest) {
        String normalizedEmail = registerRequest.getEmail().trim().toLowerCase();

        User user = User.builder()
                .name(registerRequest.getName().trim())
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(registerRequest.getPassword()))
                .roleInt(RoleMappings.BUYER)
                .enabled(true)
                .build();

        userRepository.save(user);
    }

    private void saveSeller(RegisterRequest registerRequest) {
        String normalizedEmail = registerRequest.getEmail().trim().toLowerCase();

        Seller seller = Seller.builder()
                .name(registerRequest.getName().trim())
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(registerRequest.getPassword()))
                .enabled(false)
                .build();

        sellerRepository.save(seller);
    }

    private void prepareRegisterPage(Model model,
                                     String accountType,
                                     String pageTitle,
                                     String accountTypeDescription) {
        if (!model.containsAttribute("registerRequest")) {
            model.addAttribute("registerRequest", new RegisterRequest());
        }
        model.addAttribute("accountType", accountType);
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("accountTypeDescription", accountTypeDescription);
    }
}

