package com.project.domain.upload.service;

import org.springframework.web.multipart.MultipartFile;

import com.project.domain.upload.dto.response.UploadResponse;
import com.project.domain.upload.enums.UploadType;

public interface UploadService {

    UploadResponse upload(MultipartFile file, UploadType type);
}
