package com.project.domain.example.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.project.domain.example.entity.Example;
import com.project.domain.example.infra.cache.ExampleCacheRepository;
import com.project.domain.example.repository.ExampleRepository;
import com.project.domain.example.service.port.ExampleEventPublisher;

@ExtendWith(MockitoExtension.class)
class ExampleServiceImplTest {

    @Mock private ExampleRepository exampleRepository;

    @Mock private ExampleEventPublisher exampleEventPublisher;

    @Mock private ExampleCacheRepository exampleCacheRepository;

    @InjectMocks private ExampleServiceImpl exampleService;

    @Test
    @DisplayName("findById - Example 조회 성공 (Cache Hit)")
    void findByIdReturnsExampleFromCache() {
        // given
        Long exampleId = 1L;
        Example expected =
                Example.builder()
                        .exampleId(exampleId)
                        .exampleName("test")
                        .exampleContent("content")
                        .build();
        given(exampleCacheRepository.findById(exampleId)).willReturn(Optional.of(expected));

        // when
        Example result = exampleService.findById(exampleId);

        // then
        assertThat(result).isEqualTo(expected);
        verify(exampleCacheRepository).findById(exampleId);
    }

    @Test
    @DisplayName("findById - Example 조회 성공 (Cache Miss)")
    void findByIdReturnsExampleFromDb() {
        // given
        Long exampleId = 1L;
        Example expected =
                Example.builder()
                        .exampleId(exampleId)
                        .exampleName("test")
                        .exampleContent("content")
                        .build();
        given(exampleCacheRepository.findById(exampleId)).willReturn(Optional.empty());
        given(exampleRepository.findById(exampleId)).willReturn(Optional.of(expected));

        // when
        Example result = exampleService.findById(exampleId);

        // then
        assertThat(result).isEqualTo(expected);
        verify(exampleCacheRepository).findById(exampleId);
        verify(exampleRepository).findById(exampleId);
        verify(exampleCacheRepository).save(expected);
    }

    @Test
    @DisplayName("create - Example 생성 성공 (DB + Event + Cache)")
    void createSavesExample() {
        // given
        Example savedExample =
                Example.builder()
                        .exampleId(1L)
                        .exampleName("name")
                        .exampleContent("content")
                        .build();
        given(exampleRepository.save(any(Example.class))).willReturn(savedExample);

        // when
        Example result = exampleService.create("name", "content");

        // then
        assertThat(result).isEqualTo(savedExample);
        verify(exampleRepository).save(any(Example.class));
        verify(exampleEventPublisher).publishExampleCreated(savedExample);
        verify(exampleCacheRepository).save(savedExample);
    }

    @Test
    @DisplayName("update - Example 업데이트 성공 (DB + Cache)")
    void updateSavesExample() {
        // given
        Long exampleId = 1L;
        Example existingExample =
                Example.builder()
                        .exampleId(exampleId)
                        .exampleName("old")
                        .exampleContent("old content")
                        .build();
        given(exampleRepository.findById(exampleId)).willReturn(Optional.of(existingExample));

        // when
        Example result = exampleService.update(exampleId, "new", "new content");

        // then
        assertThat(result.getExampleName()).isEqualTo("new");
        assertThat(result.getExampleContent()).isEqualTo("new content");
        verify(exampleRepository).findById(exampleId);
        verify(exampleCacheRepository).save(existingExample);
    }
}
