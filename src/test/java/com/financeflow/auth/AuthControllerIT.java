package com.financeflow.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.financeflow.auth.dto.LoginRequest;
import com.financeflow.auth.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIT {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    void register_then_login_then_access_protected() throws Exception {
        var reg = new RegisterRequest("Jane Doe", "jane@example.com", "SuperSecret123");

        var registerBody = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(reg)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken", not(emptyString())))
                .andReturn().getResponse().getContentAsString();

        var token = json.readTree(registerBody).get("accessToken").asText();

        mvc.perform(get("/api/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("jane@example.com")));

        var login = new LoginRequest("jane@example.com", "SuperSecret123");
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType", is("Bearer")));
    }

    @Test
    void reject_duplicate_email() throws Exception {
        var reg = new RegisterRequest("Dup", "dup@example.com", "SuperSecret123");
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(reg))).andExpect(status().isCreated());
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(reg)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reject_unauthenticated_me() throws Exception {
        mvc.perform(get("/api/me")).andExpect(status().isForbidden());
    }
}