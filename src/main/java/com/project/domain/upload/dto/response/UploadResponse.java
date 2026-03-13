package com.project.domain.upload.dto.response;

public record UploadResponse(String url) {

    public static UploadResponse from(String url) {
        return new UploadResponse(url);
    }
}
