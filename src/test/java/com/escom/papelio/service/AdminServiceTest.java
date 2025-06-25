package com.escom.papelio.service;

import com.escom.papelio.dto.UserDTO;
import com.escom.papelio.model.User;
import com.escom.papelio.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminService adminService;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    private User testUser;
    private UserDTO testUserDTO;

    @BeforeEach
    void setUp() {
        // Set up test data
        testUser = new User();
        testUser.setId(1L);
        testUser.setName("Test User");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setRole("ROLE_USER");

        testUserDTO = new UserDTO();
        testUserDTO.setId(1L);
        testUserDTO.setName("Test User");
        testUserDTO.setEmail("test@example.com");
        testUserDTO.setPassword("password123");
        testUserDTO.setRole("ROLE_USER");
    }

    @Test
    void shouldReturnAllUsers() {
        // Arrange
        User user2 = new User();
        user2.setId(2L);
        user2.setName("Another User");
        user2.setEmail("another@example.com");
        user2.setRole("ROLE_USER");

        when(userRepository.findAll()).thenReturn(Arrays.asList(testUser, user2));

        // Act
        List<UserDTO> result = adminService.listarUsuarios();

        // Assert
        assertEquals(2, result.size());
        assertEquals("Test User", result.get(0).getName());
        assertEquals("Another User", result.get(1).getName());
    }

    @Test
    void shouldReturnUserById() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Act
        UserDTO result = adminService.obtenerUsuarioPorId(1L);

        // Assert
        assertEquals("Test User", result.getName());
        assertEquals("test@example.com", result.getEmail());
        assertEquals("ROLE_USER", result.getRole());
    }

    @Test
    void shouldThrowExceptionWhenUserNotFound() {
        // Arrange
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            adminService.obtenerUsuarioPorId(999L);
        });

        assertTrue(exception.getMessage().contains("no encontrado"));
    }

    @Test
    void shouldCreateUserSuccessfully() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        UserDTO result = adminService.crearUsuario(testUserDTO);

        // Assert
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertEquals("test@example.com", savedUser.getEmail());
        assertEquals("Test User", savedUser.getName());
        assertEquals("encodedPassword", savedUser.getPassword());
        assertEquals("ROLE_USER", savedUser.getRole());

        assertEquals("Test User", result.getName());
        assertEquals("test@example.com", result.getEmail());
    }

    @Test
    void shouldCreateAdminUserSuccessfully() {
        // Arrange
        testUserDTO.setRole("ROLE_ADMIN");

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");

        testUser.setRole("ROLE_ADMIN");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        UserDTO result = adminService.crearUsuario(testUserDTO);

        // Assert
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertEquals("ROLE_ADMIN", savedUser.getRole());
        assertEquals("ROLE_ADMIN", result.getRole());
    }

    @Test
    void shouldThrowExceptionWhenCreatingUserWithExistingEmail() {
        // Arrange
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            adminService.crearUsuario(testUserDTO);
        });

        assertTrue(exception.getMessage().contains("email ya está registrado"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldThrowExceptionWhenCreatingUserWithoutPassword() {
        // Arrange
        testUserDTO.setPassword("");

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            adminService.crearUsuario(testUserDTO);
        });

        assertTrue(exception.getMessage().contains("contraseña es obligatoria"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldUpdateUserSuccessfully() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        testUserDTO.setName("Updated Name");

        // Act
        UserDTO result = adminService.actualizarUsuario(1L, testUserDTO);

        // Assert
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertEquals("Updated Name", savedUser.getName());
        assertEquals("Updated Name", result.getName());
    }

    @Test
    void shouldUpdateUserPasswordWhenProvided() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode("newPassword")).thenReturn("newEncodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        testUserDTO.setPassword("newPassword");

        // Act
        adminService.actualizarUsuario(1L, testUserDTO);

        // Assert
        verify(passwordEncoder).encode("newPassword");
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertEquals("newEncodedPassword", savedUser.getPassword());
    }

    @Test
    void shouldNotUpdateUserPasswordWhenEmpty() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        testUserDTO.setPassword("");

        // Act
        adminService.actualizarUsuario(1L, testUserDTO);

        // Assert
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertEquals("encodedPassword", savedUser.getPassword());
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistentUser() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            adminService.actualizarUsuario(999L, testUserDTO);
        });

        assertTrue(exception.getMessage().contains("no encontrado"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingUserWithExistingEmail() {
        // Arrange
        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setEmail("old@example.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            adminService.actualizarUsuario(1L, testUserDTO);
        });

        assertTrue(exception.getMessage().contains("email ya está registrado"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldDeleteUserSuccessfully() {
        // Arrange
        when(userRepository.existsById(1L)).thenReturn(true);

        // Act
        adminService.eliminarUsuario(1L);

        // Assert
        verify(userRepository).deleteById(1L);
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistentUser() {
        // Arrange
        when(userRepository.existsById(999L)).thenReturn(false);

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            adminService.eliminarUsuario(999L);
        });

        assertTrue(exception.getMessage().contains("no encontrado"));
        verify(userRepository, never()).deleteById(anyLong());
    }
}
