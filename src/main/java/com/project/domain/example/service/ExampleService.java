package com.project.domain.example.service;

import com.project.domain.example.entity.Example;

public interface ExampleService {
    Example findById(Long exampleId);

    Example create(String exampleName, String exampleContent);

    Example update(Long exampleId, String exampleName, String exampleContent);
}
