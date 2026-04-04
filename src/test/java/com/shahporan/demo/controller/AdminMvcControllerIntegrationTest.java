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
class AdminMvcControllerIntegrationTest {

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

    // INTEGRATION TEST 7
    @Test
    @WithUserDetails("admin@demo.com")
    void adminUsersPage_asAdmin_loadsWithAllAttributes() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("users"))
                .andExpect(model().attributeExists("buyerCount"))
                .andExpect(model().attributeExists("sellerCount"))
                .andExpect(model().attributeExists("adminCount"))
                .andExpect(model().attributeExists("pendingSellerCount"))
                .andExpect(view().name("admin/users"));
    }

    // INTEGRATION TEST 8
    @Test
    @WithMockUser(username = "admin@shopflow.com", roles = {"ADMIN"})
    void adminSellersPage_asAdmin_loadsSuccessfully() throws Exception {
        mockMvc.perform(get("/admin/sellers"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("sellers"))
                .andExpect(model().attributeExists("pendingSellerCount"))
                .andExpect(model().attributeExists("totalSellerCount"))
                .andExpect(view().name("admin/sellers"));
    }

    // INTEGRATION TEST 9
    @Test
    @WithMockUser(username = "buyer@test.com", roles = {"BUYER"})
    void adminUsersPage_asBuyer_returns403() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isForbidden());
    }

    // INTEGRATION TEST 10
    @Test
    @WithMockUser(username = "seller@test.com", roles = {"SELLER"})
    void adminSellersPage_asSeller_returns403() throws Exception {
        mockMvc.perform(get("/admin/sellers"))
                .andExpect(status().isForbidden());
    }

    // INTEGRATION TEST 11
    @Test
    void adminUsersPage_unauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }
}
