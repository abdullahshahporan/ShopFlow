package com.shahporan.demo.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class OrderMvcControllerIntegrationTest {

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

    // INTEGRATION TEST 21
    @Test
    @WithUserDetails("buyer@demo.com")
    void buyerOrdersPage_asAuthenticatedBuyer_loadsSuccessfully() throws Exception {
        mockMvc.perform(get("/buyer/orders"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("orders"))
                .andExpect(view().name("buyer/orders"));
    }

    // INTEGRATION TEST 22
    @Test
    @WithUserDetails("seller@demo.com")
    void sellerOrdersPage_asAuthenticatedSeller_loadsSuccessfully() throws Exception {
        mockMvc.perform(get("/seller/orders"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("sellerOrders"))
                .andExpect(view().name("seller/orders"));
    }

    // INTEGRATION TEST 23
    @Test
    @WithMockUser(username = "admin@test.com", roles = {"ADMIN"})
    void buyerOrdersPage_asAdmin_returns403() throws Exception {
        mockMvc.perform(get("/buyer/orders"))
                .andExpect(status().isForbidden());
    }

    // INTEGRATION TEST 24
    @Test
    void buyerOrdersPage_unauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/buyer/orders"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }
}
