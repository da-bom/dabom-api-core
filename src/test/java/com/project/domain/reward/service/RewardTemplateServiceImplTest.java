package com.project.domain.reward.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.project.domain.reward.dto.request.RewardTemplateRequest;
import com.project.domain.reward.entity.RewardTemplate;
import com.project.domain.reward.enums.RewardCategory;
import com.project.domain.reward.repository.RewardTemplateRepository;
import com.project.global.exception.ApplicationException;
import com.project.global.exception.code.RewardErrorCode;

@ExtendWith(MockitoExtension.class)
class RewardTemplateServiceImplTest {

    @Mock private RewardTemplateRepository rewardTemplateRepository;

    @InjectMocks private RewardTemplateServiceImpl rewardTemplateService;

    @Test
    @DisplayName("getAllTemplates - 삭제되지 않은 템플릿 목록을 반환한다")
    void getAllTemplates_returnsNonDeletedTemplates() {
        // given
        List<RewardTemplate> templates =
                List.of(
                        RewardTemplate.builder()
                                .name("데이터 1GB")
                                .category(RewardCategory.DATA)
                                .defaultValue(1024L)
                                .unit("MB")
                                .isSystem(false)
                                .build());
        given(rewardTemplateRepository.findAllByDeletedAtIsNull()).willReturn(templates);

        // when
        List<RewardTemplate> result = rewardTemplateService.getAllTemplates();

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("데이터 1GB");
        verify(rewardTemplateRepository).findAllByDeletedAtIsNull();
    }

    @Test
    @DisplayName("createTemplate - 새로운 템플릿을 생성하고 저장한다")
    void createTemplate_validRequest_savesAndReturnsTemplate() {
        // given
        RewardTemplateRequest.Upsert request =
                new RewardTemplateRequest.Upsert(
                        "기프티콘 5000원", RewardCategory.GIFTICON, 5000L, "원", false);
        given(rewardTemplateRepository.save(any(RewardTemplate.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        RewardTemplate result = rewardTemplateService.createTemplate(request);

        // then
        assertThat(result.getName()).isEqualTo("기프티콘 5000원");
        assertThat(result.getCategory()).isEqualTo(RewardCategory.GIFTICON);
        assertThat(result.getDefaultValue()).isEqualTo(5000L);
        assertThat(result.getUnit()).isEqualTo("원");
        verify(rewardTemplateRepository).save(any(RewardTemplate.class));
    }

    @Test
    @DisplayName("updateTemplate - 기존 템플릿을 수정한다")
    void updateTemplate_validRequest_updatesTemplate() {
        // given
        Long id = 1L;
        RewardTemplate existing =
                RewardTemplate.builder()
                        .name("기존 템플릿")
                        .category(RewardCategory.DATA)
                        .defaultValue(100L)
                        .unit("MB")
                        .isSystem(false)
                        .build();
        given(rewardTemplateRepository.findByIdAndDeletedAtIsNull(id))
                .willReturn(Optional.of(existing));

        RewardTemplateRequest.Upsert request =
                new RewardTemplateRequest.Upsert("수정된 템플릿", RewardCategory.TIME, 200L, "분", false);

        // when
        RewardTemplate result = rewardTemplateService.updateTemplate(id, request);

        // then
        assertThat(result.getName()).isEqualTo("수정된 템플릿");
        assertThat(result.getCategory()).isEqualTo(RewardCategory.TIME);
        assertThat(result.getDefaultValue()).isEqualTo(200L);
        assertThat(result.getUnit()).isEqualTo("분");
    }

    @Test
    @DisplayName("deleteTemplate - 일반 템플릿을 soft-delete 한다")
    void deleteTemplate_nonSystemTemplate_deletesSuccessfully() {
        // given
        Long id = 1L;
        RewardTemplate template =
                RewardTemplate.builder()
                        .name("일반 템플릿")
                        .category(RewardCategory.ETC)
                        .defaultValue(100L)
                        .unit("개")
                        .isSystem(false)
                        .build();
        given(rewardTemplateRepository.findByIdAndDeletedAtIsNull(id))
                .willReturn(Optional.of(template));

        // when
        rewardTemplateService.deleteTemplate(id);

        // then
        assertThat(template.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("deleteTemplate - 시스템 템플릿 삭제 시 예외가 발생한다")
    void deleteTemplate_systemTemplate_throwsException() {
        // given
        Long id = 1L;
        RewardTemplate systemTemplate =
                RewardTemplate.builder()
                        .name("시스템 템플릿")
                        .category(RewardCategory.DATA)
                        .defaultValue(100L)
                        .unit("MB")
                        .isSystem(true)
                        .build();
        given(rewardTemplateRepository.findByIdAndDeletedAtIsNull(id))
                .willReturn(Optional.of(systemTemplate));

        // when & then
        assertThatThrownBy(() -> rewardTemplateService.deleteTemplate(id))
                .isInstanceOf(ApplicationException.class)
                .satisfies(
                        e ->
                                assertThat(((ApplicationException) e).getCode())
                                        .isEqualTo(RewardErrorCode.REWARD_TEMPLATE_SYSTEM_DELETE));
    }

    @Test
    @DisplayName("updateTemplate - 존재하지 않는 템플릿 수정 시 예외가 발생한다")
    void updateTemplate_notFoundId_throwsException() {
        // given
        Long id = 999L;
        given(rewardTemplateRepository.findByIdAndDeletedAtIsNull(id)).willReturn(Optional.empty());

        RewardTemplateRequest.Upsert request =
                new RewardTemplateRequest.Upsert("템플릿", RewardCategory.DATA, 100L, "MB", false);

        // when & then
        assertThatThrownBy(() -> rewardTemplateService.updateTemplate(id, request))
                .isInstanceOf(ApplicationException.class)
                .satisfies(
                        e ->
                                assertThat(((ApplicationException) e).getCode())
                                        .isEqualTo(RewardErrorCode.REWARD_TEMPLATE_NOT_FOUND));
    }
}
