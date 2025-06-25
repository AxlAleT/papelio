package com.escom.papelio.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class DashboardControllerTest {

    @Mock
    private Authentication authentication;

    @InjectMocks
    private DashboardController dashboardController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(dashboardController).build();
    }

    @Test
    void dashboardShouldRedirectToAdminWhenUserIsAdmin() throws Exception {
        // Set up the authentication to return ROLE_ADMIN
        when(authentication.getAuthorities()).thenReturn(
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")));

        mockMvc.perform(get("/dashboard").principal(authentication))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"));
    }

    @Test
    void dashboardShouldRedirectToUserWhenUserIsNotAdmin() throws Exception {
        // Set up the authentication to return ROLE_USER
        when(authentication.getAuthorities()).thenReturn(
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));

        mockMvc.perform(get("/dashboard").principal(authentication))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user"));
    }
}
