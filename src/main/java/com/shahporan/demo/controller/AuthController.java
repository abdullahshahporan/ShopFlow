package com.shahporan.demo.controller;

import com.shahporan.demo.dto.RegisterRequest;
import com.shahporan.demo.entity.User;
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

@Controller
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        if (!model.containsAttribute("registerRequest")) {
            model.addAttribute("registerRequest", new RegisterRequest());
        }
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerRequest") RegisterRequest registerRequest,
                           BindingResult bindingResult) {
        String normalizedEmail = registerRequest.getEmail() == null ? ""
                : registerRequest.getEmail().trim().toLowerCase();

        if (!registerRequest.getPassword().equals(registerRequest.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "password.mismatch", "Passwords do not match");
        }

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            bindingResult.rejectValue("email", "email.duplicate", "Email is already registered");
        }

        if (bindingResult.hasErrors()) {
            return "register";
        }

        User user = User.builder()
                .name(registerRequest.getName().trim())
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(registerRequest.getPassword()))
                .roleInt(RoleMappings.BUYER)
                .enabled(true)
                .build();

        userRepository.save(user);
        return "redirect:/login?registered";
    }
}

