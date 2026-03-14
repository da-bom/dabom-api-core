package com.project.domain.upload.service;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.project.common.exception.ApplicationException;
import com.project.common.exception.code.UploadErrorCode;
import com.project.domain.upload.enums.UploadType;

import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UploadServiceImpl implements UploadService {

    private static final Set<String> ALLOWED_MIME_TYPES =
            Set.of("image/png", "image/jpeg", "image/webp");
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024; // 5MB

    private final S3Client s3Client;

    @Value("${r2.bucket}")
    private String bucket;

    @Value("${r2.cdn-base-url}")
    private String cdnBaseUrl;

    @Override
    public String upload(MultipartFile file, UploadType type) {
        validateFile(file);

        String extension = extractExtension(file.getContentType());
        String fileName = UUID.randomUUID() + "." + extension;
        String key = type.getPrefix() + "/" + fileName;

        try {
            PutObjectRequest putRequest =
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(file.getContentType())
                            .build();

            s3Client.putObject(
                    putRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (IOException | SdkException e) {
            throw new ApplicationException(UploadErrorCode.UPLOAD_FAILED);
        }

        return cdnBaseUrl + "/" + key;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApplicationException(UploadErrorCode.FILE_REQUIRED);
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ApplicationException(UploadErrorCode.FILE_TOO_LARGE);
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType)) {
            throw new ApplicationException(UploadErrorCode.INVALID_MIME_TYPE);
        }
    }

    private String extractExtension(String contentType) {
        if ("image/png".equals(contentType)) {
            return "png";
        }
        if ("image/jpeg".equals(contentType)) {
            return "jpg";
        }
        if ("image/webp".equals(contentType)) {
            return "webp";
        }
        throw new ApplicationException(UploadErrorCode.INVALID_MIME_TYPE);
    }
}
