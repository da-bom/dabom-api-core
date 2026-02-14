package com.project.domain.example.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.domain.example.dto.request.CreateExampleRequest;
import com.project.domain.example.dto.request.UpdateExampleRequest;
import com.project.domain.example.dto.response.ExampleDetailResponse;
import com.project.domain.example.dto.response.ExampleResponse;
import com.project.domain.example.entity.Example;
import com.project.domain.example.service.ExampleService;
import com.project.global.api.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/example")
@RequiredArgsConstructor
public class ExampleController {

    private final ExampleService exampleService;

    @GetMapping("/{exampleId}")
    public ApiResponse<ExampleDetailResponse> findById(@PathVariable Long exampleId) {
        Example example = exampleService.findById(exampleId);
        return ApiResponse.success(ExampleDetailResponse.from(example));
    }

    @PostMapping
    public ApiResponse<ExampleResponse> create(@RequestBody CreateExampleRequest request) {
        Example createdExample =
                exampleService.create(request.exampleName(), request.exampleContent());
        return ApiResponse.created(ExampleResponse.from(createdExample));
    }

    @PutMapping("/{exampleId}")
    public ApiResponse<ExampleResponse> update(
            @PathVariable Long exampleId, @RequestBody UpdateExampleRequest request) {
        Example updatedExample =
                exampleService.update(exampleId, request.exampleName(), request.exampleContent());
        return ApiResponse.success(ExampleResponse.from(updatedExample));
    }
}
