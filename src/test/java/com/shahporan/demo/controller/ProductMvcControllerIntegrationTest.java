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
class ProductMvcControllerIntegrationTest {

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

    // INTEGRATION TEST 12
    @Test
    void productsPage_unauthenticated_loadsSuccessfully() throws Exception {
        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("products"))
                .andExpect(view().name("products"));
    }

    // INTEGRATION TEST 13
    @Test
    @WithMockUser(username = "buyer@test.com", roles = {"BUYER"})
    void productsPage_asAuthenticatedBuyer_loadsSuccessfully() throws Exception {
        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("products"))
                .andExpect(view().name("products"));
    }

    // INTEGRATION TEST 14
    @Test
    @WithMockUser(username = "seller@test.com", roles = {"SELLER"})
    void sellerProductsPage_asSeller_loadsSuccessfully() throws Exception {
        mockMvc.perform(get("/seller/products"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("products"))
                .andExpect(view().name("seller/products"));
    }

    // INTEGRATION TEST 15
    @Test
    @WithMockUser(username = "seller@test.com", roles = {"SELLER"})
    void sellerProductFormPage_asSeller_loadsSuccessfully() throws Exception {
        mockMvc.perform(get("/seller/products/new"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("product"))
                .andExpect(view().name("seller/product-form"));
    }

    // INTEGRATION TEST 16
    @Test
    @WithMockUser(username = "buyer@test.com", roles = {"BUYER"})
    void sellerProductsPage_asBuyer_returns403() throws Exception {
        mockMvc.perform(get("/seller/products"))
                .andExpect(status().isForbidden());
    }
}
