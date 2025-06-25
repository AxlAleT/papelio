package com.escom.papelio.controller;

import com.escom.papelio.dto.UserDTO;
import com.escom.papelio.service.AdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class AdminControllerTest {

    @Mock
    private AdminService adminService;

    @InjectMocks
    private AdminController adminController;

    private MockMvc mockMvc;
    private List<UserDTO> userList;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminController).build();

        // Create test users
        UserDTO user1 = new UserDTO();
        user1.setId(1L);
        user1.setName("Test User");
        user1.setEmail("test@example.com");

        UserDTO user2 = new UserDTO();
        user2.setId(2L);
        user2.setName("Second User");
        user2.setEmail("second@example.com");

        userList = Arrays.asList(user1, user2);
    }

    @Test
    void adminPanelShouldReturnAdminPanelView() throws Exception {
        // Arrange
        when(adminService.listarUsuarios()).thenReturn(userList);

        // Act & Assert
        mockMvc.perform(get("/admin"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/panel"))
                .andExpect(model().attribute("usuarios", userList));
    }

    @Test
    void gestionUsuariosShouldReturnUsersView() throws Exception {
        // Arrange
        when(adminService.listarUsuarios()).thenReturn(userList);

        // Act & Assert
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users"))
                .andExpect(model().attribute("usuarios", userList));
    }
}
