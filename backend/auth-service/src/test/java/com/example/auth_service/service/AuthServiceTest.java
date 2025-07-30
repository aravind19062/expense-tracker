package com.example.auth_service.service;

import com.example.auth_service.model.*;
import com.example.auth_service.repository.UserRepository;
import com.example.auth_service.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;


    @Test
    void testRegister_Success() {
        RegisterRequest request = new RegisterRequest();
        request.setUserName("Arvind");
        request.setEmail("arvind@mail.com");
        request.setPassword("pass123");


        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode("pass123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenReturn(new User());

        String result = authService.register(request);

        assertEquals("User registered successfully", result);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testRegister_AlreadyExists() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("arvind@mail.com");

        when(userRepository.findByEmail(request.getEmail()))
                .thenReturn(Optional.of(new User()));

        assertThrows(RuntimeException.class, () -> authService.register(request));
    }

    @Test
    void testLogin_Success() {
        LoginRequest request = new LoginRequest();
        request.setEmail("arvind@mail.com");
        request.setPassword("pass123");

        User user = User.builder()
                .email("arvind@mail.com")
                .password("hashedpass")
                .build();

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pass123", "hashedpass")).thenReturn(true);
        when(jwtUtil.generateToken(user.getEmail())).thenReturn("token123");

        String token = authService.login(request);

        assertEquals("token123", token);
    }

    @Test
    void testLogin_InvalidCredentials_EmailNotFound() {
        LoginRequest request = new LoginRequest();
        request.setEmail("wrong@mail.com");

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> authService.login(request));
    }

    @Test
    void testLogin_InvalidCredentials_WrongPassword() {
        LoginRequest request = new LoginRequest();
        request.setEmail("arvind@mail.com");
        request.setPassword("wrong");

        User user = new User();
        user.setEmail("arvind@mail.com");
        user.setPassword("correct");

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "correct")).thenReturn(false);

        assertThrows(RuntimeException.class, () -> authService.login(request));
    }
}
