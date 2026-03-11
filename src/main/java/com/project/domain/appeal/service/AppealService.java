package com.project.domain.appeal.service;

import com.project.domain.appeal.dto.request.AppealCreateRequest;
import com.project.domain.appeal.dto.request.AppealCommentRequest;
import com.project.domain.appeal.dto.request.AppealRespondRequest;
import com.project.domain.appeal.dto.request.EmergencyQuotaRequest;
import com.project.domain.appeal.enums.AppealStatus;
import com.project.domain.appeal.model.AppealCancelResult;
import com.project.domain.appeal.model.AppealCommentResult;
import com.project.domain.appeal.model.AppealCreateResult;
import com.project.domain.appeal.model.AppealDetailResult;
import com.project.domain.appeal.model.AppealListResult;
import com.project.domain.appeal.model.AppealRespondResult;
import com.project.domain.appeal.model.EmergencyQuotaResult;
import com.project.global.auth.model.AuthContext;

/** 이의제기 서비스 명세 */
public interface AppealService {
    /** 이의제기 목록 조회 */
    AppealListResult getAppeals(AuthContext auth, AppealStatus status, Long cursor, int size);

    /** 이의제기 상세 조회 */
    AppealDetailResult getAppealDetail(AuthContext auth, Long appealId, Long cursor, int size);

    /** 이의제기 생성 */
    AppealCreateResult createAppeal(AuthContext auth, AppealCreateRequest request);

    /** 이의제기 승인/거절 */
    AppealRespondResult respondAppeal(AuthContext auth, Long appealId, AppealRespondRequest request);

    /** 이의제기 댓글 작성 */
    AppealCommentResult createComment(AuthContext auth, Long appealId, AppealCommentRequest request);

    /** 이의제기 취소 */
    AppealCancelResult cancelAppeal(AuthContext auth, Long appealId);

    /** 긴급 쿼터 요청 */
    EmergencyQuotaResult requestEmergencyQuota(AuthContext auth, EmergencyQuotaRequest request);
}
