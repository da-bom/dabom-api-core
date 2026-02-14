package com.project.domain.example.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.domain.example.entity.Example;
import com.project.domain.example.infra.cache.ExampleCacheRepository;
import com.project.domain.example.repository.ExampleRepository;
import com.project.domain.example.service.port.ExampleEventPublisher;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExampleServiceImpl implements ExampleService {

    private final ExampleRepository exampleRepository;
    private final ExampleEventPublisher exampleEventPublisher;
    private final ExampleCacheRepository exampleCacheRepository;

    @Override
    public Example findById(Long exampleId) {
        return exampleCacheRepository
                .findById(exampleId)
                .orElseGet(
                        () -> {
                            Example example =
                                    exampleRepository
                                            .findById(exampleId)
                                            .orElseThrow(
                                                    () ->
                                                            new IllegalArgumentException(
                                                                    "Example not found: "
                                                                            + exampleId));
                            exampleCacheRepository.save(example);
                            return example;
                        });
    }

    @Override
    @Transactional
    public Example create(String exampleName, String exampleContent) {
        Example example = Example.create(exampleName, exampleContent);
        Example savedExample = exampleRepository.save(example);

        exampleEventPublisher.publishExampleCreated(savedExample);
        exampleCacheRepository.save(savedExample);

        return savedExample;
    }

    @Override
    @Transactional
    public Example update(Long exampleId, String exampleName, String exampleContent) {
        Example example =
                exampleRepository
                        .findById(exampleId)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Example not found: " + exampleId));

        example.update(exampleName, exampleContent);

        exampleCacheRepository.save(example);

        return example;
    }
}
