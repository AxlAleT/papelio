package com.escom.papelio.controller;

import com.escom.papelio.dto.UserDTO;
import com.escom.papelio.service.AdminService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class AdminRESTControllerTest {

    @Mock
    private AdminService adminService;

    @InjectMocks
    private AdminRESTController adminRESTController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private UserDTO userDTO;
    private List<UserDTO> userList;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminRESTController).build();
        objectMapper = new ObjectMapper();

        // Create test user
        userDTO = new UserDTO();
        userDTO.setId(1L);
        userDTO.setName("Test User");
        userDTO.setEmail("test@example.com");

        // Create user list for testing
        UserDTO secondUser = new UserDTO();
        secondUser.setId(2L);
        secondUser.setName("Second User");
        secondUser.setEmail("second@example.com");

        userList = Arrays.asList(userDTO, secondUser);
    }

    @Test
    void listarUsuariosShouldReturnAllUsers() throws Exception {
        when(adminService.listarUsuarios()).thenReturn(userList);

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Test User"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].name").value("Second User"));
    }

    @Test
    void obtenerUsuarioShouldReturnSpecificUser() throws Exception {
        when(adminService.obtenerUsuarioPorId(1L)).thenReturn(userDTO);

        mockMvc.perform(get("/api/admin/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Test User"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void crearUsuarioShouldReturnCreatedUser() throws Exception {
        when(adminService.crearUsuario(any(UserDTO.class))).thenReturn(userDTO);

        mockMvc.perform(post("/api/admin/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Test User"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void actualizarUsuarioShouldReturnUpdatedUser() throws Exception {
        UserDTO updatedUser = new UserDTO();
        updatedUser.setId(1L);
        updatedUser.setName("Updated User");
        updatedUser.setEmail("test@example.com");

        when(adminService.actualizarUsuario(anyLong(), any(UserDTO.class))).thenReturn(updatedUser);

        mockMvc.perform(put("/api/admin/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Updated User"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void eliminarUsuarioShouldReturnNoContent() throws Exception {
        doNothing().when(adminService).eliminarUsuario(anyLong());

        mockMvc.perform(delete("/api/admin/users/1"))
                .andExpect(status().isNoContent());
    }
}
