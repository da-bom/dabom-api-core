package com.project.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.project.global.auth.JwtTokenUtil;

@WebMvcTest(HomeController.class)
class HomeControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private JwtTokenUtil jwtTokenUtil;

    @Test
    @WithMockUser
    @DisplayName("GET / - API 기본 정보 반환")
    void homeReturnsApiInfo() throws Exception {
        mockMvc.perform(get("/"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ok"))
                .andExpect(jsonPath("$.data.version").exists())
                .andExpect(jsonPath("$.data.docs").value("/swagger-ui.html"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET / - message에 'running' 포함")
    void homeMessageContainsRunning() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.data.message")
                                .value(org.hamcrest.Matchers.containsString("running")));
    }
}
