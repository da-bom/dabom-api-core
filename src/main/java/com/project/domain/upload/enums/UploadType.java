package com.project.domain.upload.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UploadType {
    REWARD("rewards"),
    PROFILE("profiles"),
    MISSION("missions");

    private final String prefix;
}
