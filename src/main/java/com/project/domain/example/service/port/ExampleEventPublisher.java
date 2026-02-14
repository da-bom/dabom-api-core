package com.project.domain.example.service.port;

import com.project.domain.example.entity.Example;

public interface ExampleEventPublisher {
    void publishExampleCreated(Example example);
}
