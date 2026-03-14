package com.project.domain.upload.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.project.common.api.response.ApiResponse;
import com.project.common.auth.aop.AdminOnly;
import com.project.domain.upload.dto.response.UploadResponse;
import com.project.domain.upload.enums.UploadType;
import com.project.domain.upload.service.UploadService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/uploads")
@RequiredArgsConstructor
@Tag(name = "Upload", description = "파일 업로드 API")
public class UploadController {

    private final UploadService uploadService;

    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @AdminOnly
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "이미지 업로드", description = "이미지를 R2에 업로드하고 CDN URL을 반환합니다.")
    public ApiResponse<UploadResponse> uploadImage(
            @RequestParam("file") MultipartFile file, @RequestParam("type") UploadType type) {

        String cdnUrl = uploadService.upload(file, type);
        return ApiResponse.created(UploadResponse.from(cdnUrl));
    }
}
