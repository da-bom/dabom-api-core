package com.project.domain.example.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.domain.example.dto.request.CreateExampleRequest;
import com.project.domain.example.dto.request.UpdateExampleRequest;
import com.project.domain.example.entity.Example;
import com.project.domain.example.service.ExampleService;

@WebMvcTest(ExampleController.class)
class ExampleControllerTest {

    @Autowired private MockMvc mockMvc;

    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private ExampleService exampleService;

    @Test
    @WithMockUser
    @DisplayName("GET /example/{id} - Example 조회 성공")
    void findByIdReturnsOk() throws Exception {
        // given
        Long exampleId = 1L;
        Example example =
                Example.builder()
                        .exampleId(exampleId)
                        .exampleName("test")
                        .exampleContent("content")
                        .build();
        given(exampleService.findById(exampleId)).willReturn(example);

        // when & then
        mockMvc.perform(get("/example/{exampleId}", exampleId))
                .andDo(print())
                .andExpect(status().isOk());

        verify(exampleService).findById(exampleId);
    }

    @Test
    @WithMockUser
    @DisplayName("POST /example - Example 생성 성공")
    void createReturnsCreated() throws Exception {
        // given
        CreateExampleRequest request = new CreateExampleRequest("name", "content");
        Example createdExample =
                Example.builder()
                        .exampleId(1L)
                        .exampleName("name")
                        .exampleContent("content")
                        .build();
        given(exampleService.create("name", "content")).willReturn(createdExample);

        // when & then
        mockMvc.perform(
                        post("/example")
                                .with(SecurityMockMvcRequestPostProcessors.csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk());

        verify(exampleService).create("name", "content");
    }

    @Test
    @WithMockUser
    @DisplayName("PUT /example/{id} - Example 업데이트 성공")
    void updateReturnsOk() throws Exception {
        // given
        Long exampleId = 1L;
        UpdateExampleRequest request = new UpdateExampleRequest("updated", "updated content");
        Example updatedExample =
                Example.builder()
                        .exampleId(exampleId)
                        .exampleName("updated")
                        .exampleContent("updated content")
                        .build();
        given(exampleService.update(exampleId, "updated", "updated content"))
                .willReturn(updatedExample);

        // when & then
        mockMvc.perform(
                        put("/example/{exampleId}", exampleId)
                                .with(SecurityMockMvcRequestPostProcessors.csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk());

        verify(exampleService).update(exampleId, "updated", "updated content");
    }
}
