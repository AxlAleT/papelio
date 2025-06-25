package com.escom.papelio.controller;

import com.escom.papelio.dto.RegisterDTO;
import com.escom.papelio.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.BindingResult;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class AuthControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
    }

    @Test
    void redirectToLoginShouldRedirectToLoginPage() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("/login"));
    }

    @Test
    void showRegisterFormShouldDisplayRegisterPage() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("usuario"));
    }

    @Test
    void registerUserShouldRedirectToLoginOnSuccess() throws Exception {
        // Mock the UserService to not throw an exception
        doNothing().when(userService).registrarUsuario(any(RegisterDTO.class));

        mockMvc.perform(post("/register")
                .param("name", "Test User")
                .param("email", "test@example.com")
                .param("password", "Password123!")
                .param("confirmPassword", "Password123!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attributeExists("success"));

        verify(userService).registrarUsuario(any(RegisterDTO.class));
    }

    @Test
    void registerUserShouldStayOnRegisterPageWhenValidationFails() throws Exception {
        mockMvc.perform(post("/register")
                .param("name", "")  // Empty name to trigger validation error
                .param("email", "test@example.com")
                .param("password", "Password123!")
                .param("confirmPassword", "Password123!"))
                .andExpect(view().name("register"));

        verify(userService, never()).registrarUsuario(any(RegisterDTO.class));
    }

    @Test
    void registerUserShouldRedirectToRegisterOnServiceException() throws Exception {
        // Mock the UserService to throw an exception
        doThrow(new RuntimeException("Email already exists")).when(userService).registrarUsuario(any(RegisterDTO.class));

        mockMvc.perform(post("/register")
                .param("name", "Test User")
                .param("email", "test@example.com")
                .param("password", "Password123!")
                .param("confirmPassword", "Password123!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/register"))
                .andExpect(flash().attributeExists("error"));

        verify(userService).registrarUsuario(any(RegisterDTO.class));
    }

    @Test
    void showLoginShouldDisplayLoginPage() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    void showLoginWithErrorShouldDisplayErrorMessage() throws Exception {
        mockMvc.perform(get("/login").param("error", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attributeExists("error"));
    }
}
