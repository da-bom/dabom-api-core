package com.project.domain.appeal.service;

import com.project.domain.appeal.enums.AppealStatus;
import com.project.domain.appeal.model.AppealListResult;
import com.project.global.auth.model.AuthContext;

/** 이의제기 목록 조회 서비스 명세 */
public interface AppealService {
    /** 이의제기 목록 조회 */
    AppealListResult getAppeals(AuthContext auth, AppealStatus status, Long cursor, int size);
}
