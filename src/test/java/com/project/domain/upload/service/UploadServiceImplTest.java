package com.project.domain.upload.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import com.project.domain.upload.enums.UploadType;
import com.project.global.exception.ApplicationException;
import com.project.global.exception.code.UploadErrorCode;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

@ExtendWith(MockitoExtension.class)
class UploadServiceImplTest {

    @Mock private S3Client s3Client;

    @InjectMocks private UploadServiceImpl uploadService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(uploadService, "bucket", "test-bucket");
        ReflectionTestUtils.setField(uploadService, "cdnBaseUrl", "https://cdn.test.com");
    }

    @Test
    @DisplayName("upload - 유효한 이미지 파일을 업로드하면 CDN URL을 반환한다")
    void upload_validFile_returnsCdnUrl() {
        // given
        MockMultipartFile file =
                new MockMultipartFile("file", "test.png", "image/png", new byte[1024]);
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        // when
        String result = uploadService.upload(file, UploadType.REWARD);

        // then
        assertThat(result).startsWith("https://cdn.test.com/rewards/").endsWith(".png");
        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("upload - 파일이 null이면 FILE_REQUIRED 예외가 발생한다")
    void upload_nullFile_throwsFileRequired() {
        // when & then
        assertThatThrownBy(() -> uploadService.upload(null, UploadType.REWARD))
                .isInstanceOf(ApplicationException.class)
                .satisfies(
                        e ->
                                assertThat(((ApplicationException) e).getCode())
                                        .isEqualTo(UploadErrorCode.FILE_REQUIRED));
    }

    @Test
    @DisplayName("upload - 빈 파일이면 FILE_REQUIRED 예외가 발생한다")
    void upload_emptyFile_throwsFileRequired() {
        // given
        MockMultipartFile file =
                new MockMultipartFile("file", "test.png", "image/png", new byte[0]);

        // when & then
        assertThatThrownBy(() -> uploadService.upload(file, UploadType.REWARD))
                .isInstanceOf(ApplicationException.class)
                .satisfies(
                        e ->
                                assertThat(((ApplicationException) e).getCode())
                                        .isEqualTo(UploadErrorCode.FILE_REQUIRED));
    }

    @Test
    @DisplayName("upload - 5MB를 초과하면 FILE_TOO_LARGE 예외가 발생한다")
    void upload_fileTooLarge_throwsFileTooLarge() {
        // given
        byte[] largeContent = new byte[5 * 1024 * 1024 + 1];
        MockMultipartFile file =
                new MockMultipartFile("file", "large.png", "image/png", largeContent);

        // when & then
        assertThatThrownBy(() -> uploadService.upload(file, UploadType.REWARD))
                .isInstanceOf(ApplicationException.class)
                .satisfies(
                        e ->
                                assertThat(((ApplicationException) e).getCode())
                                        .isEqualTo(UploadErrorCode.FILE_TOO_LARGE));
    }

    @Test
    @DisplayName("upload - 지원하지 않는 MIME 타입이면 INVALID_MIME_TYPE 예외가 발생한다")
    void upload_invalidMimeType_throwsInvalidMimeType() {
        // given
        MockMultipartFile file =
                new MockMultipartFile("file", "test.gif", "image/gif", new byte[1024]);

        // when & then
        assertThatThrownBy(() -> uploadService.upload(file, UploadType.REWARD))
                .isInstanceOf(ApplicationException.class)
                .satisfies(
                        e ->
                                assertThat(((ApplicationException) e).getCode())
                                        .isEqualTo(UploadErrorCode.INVALID_MIME_TYPE));
    }

    @Test
    @DisplayName("upload - S3 업로드 실패 시 UPLOAD_FAILED 예외가 발생한다")
    void upload_s3Failure_throwsUploadFailed() {
        // given
        MockMultipartFile file =
                new MockMultipartFile("file", "test.png", "image/png", new byte[1024]);
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(S3Exception.builder().message("connection refused").build());

        // when & then
        assertThatThrownBy(() -> uploadService.upload(file, UploadType.REWARD))
                .isInstanceOf(ApplicationException.class)
                .satisfies(
                        e ->
                                assertThat(((ApplicationException) e).getCode())
                                        .isEqualTo(UploadErrorCode.UPLOAD_FAILED));
    }

    @Test
    @DisplayName("upload - UploadType에 따라 올바른 prefix가 CDN URL에 포함된다")
    void upload_differentTypes_usesCorrectPrefix() {
        // given
        MockMultipartFile file =
                new MockMultipartFile("file", "test.webp", "image/webp", new byte[1024]);
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        // when
        String result = uploadService.upload(file, UploadType.MISSION);

        // then
        assertThat(result).startsWith("https://cdn.test.com/missions/").endsWith(".webp");
    }

    @Test
    @DisplayName("upload - image/jpeg Content-Type이면 .jpg 확장자로 변환된다")
    void upload_jpegContentType_returnsJpgExtension() {
        // given
        MockMultipartFile file =
                new MockMultipartFile("file", "photo.jpeg", "image/jpeg", new byte[1024]);
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        // when
        String result = uploadService.upload(file, UploadType.PROFILE);

        // then
        assertThat(result).startsWith("https://cdn.test.com/profiles/").endsWith(".jpg");
    }
}
