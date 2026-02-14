package com.project.domain.example.dto.response;

import com.project.domain.example.entity.Example;

public record ExampleResponse(Long exampleId, String exampleName) {
    public static ExampleResponse from(Example example) {
        return new ExampleResponse(example.getExampleId(), example.getExampleName());
    }
}
