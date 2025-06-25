package com.escom.papelio.controller;

import com.escom.papelio.model.User;
import com.escom.papelio.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.security.Principal;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class UserControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private Principal principal;

    @InjectMocks
    private UserController userController;

    private MockMvc mockMvc;
    private User testUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController).build();

        // Set up test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setName("Test User");
        testUser.setEmail("test@example.com");

        // Mock principal to return test email
        when(principal.getName()).thenReturn("test@example.com");
    }

    @Test
    void redirectToDashboardShouldReturnDashboardView() throws Exception {
        mockMvc.perform(get("/user"))
                .andExpect(status().isOk())
                .andExpect(view().name("user/dashboard"));
    }

    @Test
    void dashboardShouldReturnDashboardView() throws Exception {
        mockMvc.perform(get("/user/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("user/dashboard"));
    }

    @Test
    void showProfileShouldReturnProfileViewWithUserData() throws Exception {
        // Arrange
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));

        // Act & Assert
        mockMvc.perform(get("/user/profile").principal(principal))
                .andExpect(status().isOk())
                .andExpect(view().name("user/profile"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attribute("user", testUser));
    }

    @Test
    void showProfileShouldThrowExceptionWhenUserNotFound() throws Exception {
        // Arrange
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/user/profile").principal(principal))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void showSearchPageShouldReturnSearchView() throws Exception {
        mockMvc.perform(get("/user/search"))
                .andExpect(status().isOk())
                .andExpect(view().name("user/search"));
    }
}
