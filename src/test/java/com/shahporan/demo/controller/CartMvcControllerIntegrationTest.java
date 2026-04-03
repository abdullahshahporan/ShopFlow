package com.shahporan.demo.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class CartMvcControllerIntegrationTest {

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

    // INTEGRATION TEST 17
    @Test
    @WithMockUser(username = "buyer@test.com", roles = {"BUYER"})
    void cartPage_asAuthenticatedBuyer_loadsSuccessfully() throws Exception {
        mockMvc.perform(get("/buyer/cart"))
                .andExpect(status().isOk())
                .andExpect(view().name("buyer/cart"));
    }

    // INTEGRATION TEST 18
    @Test
    @WithMockUser(username = "buyer@test.com", roles = {"BUYER"})
    void orderFormPage_asAuthenticatedBuyer_loadsSuccessfully() throws Exception {
        mockMvc.perform(get("/buyer/orders/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("buyer/order-form"));
    }

    // INTEGRATION TEST 19
    @Test
    @WithMockUser(username = "seller@test.com", roles = {"SELLER"})
    void cartPage_asSeller_returns403() throws Exception {
        mockMvc.perform(get("/buyer/cart"))
                .andExpect(status().isForbidden());
    }

    // INTEGRATION TEST 20
    @Test
    void cartPage_unauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/buyer/cart"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }
}
