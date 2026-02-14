package com.project.domain.example.dto.response;

import com.project.domain.example.entity.Example;

public record ExampleDetailResponse(Long exampleId, String exampleName, String exampleContent) {
    public static ExampleDetailResponse from(Example example) {
        return new ExampleDetailResponse(
                example.getExampleId(), example.getExampleName(), example.getExampleContent());
    }
}
