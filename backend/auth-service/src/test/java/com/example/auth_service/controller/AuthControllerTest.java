package com.example.auth_service.controller;

import com.example.auth_service.Exception.UserAlreadyExistsException;
import com.example.auth_service.Exception.WrongCredException;
import com.example.auth_service.model.LoginRequest;
import com.example.auth_service.model.RegisterRequest;
import com.example.auth_service.service.AuthService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwsHeader;
import org.aspectj.lang.annotation.Before;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testLoginSuccess() throws Exception{
        LoginRequest loginRequest=new LoginRequest();
        loginRequest.setEmail("arvind@gmail.com");
        loginRequest.setPassword("123456");
        String token = "jwt token";

        when(authService.login(any(LoginRequest.class))).thenReturn(token);

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(token));
    }
    @Test
    void testRegisterSuccess() throws Exception {
        RegisterRequest registerRequest=new RegisterRequest();
        registerRequest.setEmail("xyz@Gmail.com");
        registerRequest.setUserName("xyz");
        registerRequest.setPassword("xyz123");
        when(authService.register(any(RegisterRequest.class))).thenReturn("User registered successfully");

        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().string("User registered successfully"));
    }
    @Test
    void testLoginInvalidCredentials() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("xyz@Gmail.com");
        loginRequest.setPassword("xyz123");

        when(authService.login(any(LoginRequest.class))).thenThrow(new WrongCredException("invalid credentials"));
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized()).andExpect(content().string("invalid credentials"));
    }
    @Test
    void testRegisterAlreadyExistingUser() throws Exception{
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail("xyz@Gmail.com");
        registerRequest.setUserName("xyz");
        registerRequest.setPassword("xyz123");

        when(authService.register(any(RegisterRequest.class))).thenThrow(new UserAlreadyExistsException("User already exists"));
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("User already exists"));

    }
}
