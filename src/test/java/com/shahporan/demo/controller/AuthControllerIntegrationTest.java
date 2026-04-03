package com.shahporan.demo.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class AuthControllerIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    // INTEGRATION TEST 1
    @Test
    void loginPage_loadsSuccessfully() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    // INTEGRATION TEST 2
    @Test
    void buyerRegistrationPage_loadsSuccessfully() throws Exception {
        mockMvc.perform(get("/register/buyer"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("registerRequest"))
                .andExpect(model().attribute("accountType", "BUYER"));
    }

    // INTEGRATION TEST 3
    @Test
    void sellerRegistrationPage_loadsSuccessfully() throws Exception {
        mockMvc.perform(get("/register/seller"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("registerRequest"))
                .andExpect(model().attribute("accountType", "SELLER"));
    }

    // INTEGRATION TEST 4
    @Test
    void buyerRegistration_withValidData_redirectsToLogin() throws Exception {
        mockMvc.perform(post("/register/buyer")
                        .param("name", "Integration Test Buyer")
                        .param("email", "integration-buyer@test.com")
                        .param("password", "password123")
                        .param("confirmPassword", "password123")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registeredBuyer"));
    }

    // INTEGRATION TEST 5
    @Test
    void sellerRegistration_withValidData_redirectsToLogin() throws Exception {
        mockMvc.perform(post("/register/seller")
                        .param("name", "Integration Test Seller")
                        .param("email", "integration-seller@test.com")
                        .param("password", "password123")
                        .param("confirmPassword", "password123")
                        .param("businessName", "Test Business")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?sellerPending"));
    }

    // INTEGRATION TEST 6
    @Test
    void registration_withMismatchedPasswords_showsError() throws Exception {
        mockMvc.perform(post("/register/buyer")
                        .param("name", "Test Buyer")
                        .param("email", "mismatch@test.com")
                        .param("password", "password123")
                        .param("confirmPassword", "different456")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
    }
}
