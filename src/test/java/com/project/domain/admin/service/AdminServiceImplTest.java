package com.project.domain.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.project.domain.admin.repository.AdminRepository;
import com.project.domain.customer.enums.RoleType;
import com.project.global.auth.JwtTokenUtil;
import com.project.global.auth.TokenRefreshResult;
import com.project.global.exception.ApplicationException;
import com.project.global.exception.code.AdminErrorCode;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {

    @Mock private AdminRepository adminRepository;
    @Mock private JwtTokenUtil jwtTokenUtil;

    @InjectMocks private AdminServiceImpl adminService;

    @Test
    @DisplayName("refreshToken - 유효한 refresh token이면 새 토큰 쌍을 반환한다")
    void refreshToken_validRefreshToken_returnsNewTokenPair() {
        // given
        Claims claims = mock(Claims.class);
        given(claims.get("role", String.class)).willReturn("ADMIN");
        given(claims.getSubject()).willReturn("1");

        given(jwtTokenUtil.verifyRefreshToken("valid-refresh-token")).willReturn(claims);
        given(adminRepository.existsById(1L)).willReturn(true);
        given(jwtTokenUtil.reissueTokens(1L, RoleType.ADMIN))
                .willReturn(new TokenRefreshResult("new-access", "new-refresh", 1800L));

        // when
        TokenRefreshResult result = adminService.refreshToken("valid-refresh-token");

        // then
        assertThat(result.accessToken()).isEqualTo("new-access");
        assertThat(result.refreshToken()).isEqualTo("new-refresh");
        assertThat(result.expiresIn()).isEqualTo(1800L);
    }

    @Test
    @DisplayName("refreshToken - access token을 사용하면 예외를 던진다")
    void refreshToken_accessToken_throwsException() {
        // given
        given(jwtTokenUtil.verifyRefreshToken("access-token"))
                .willThrow(new JwtException("리프레시 토큰이 아닙니다."));

        // when & then
        assertThatThrownBy(() -> adminService.refreshToken("access-token"))
                .isInstanceOf(ApplicationException.class)
                .satisfies(
                        e ->
                                assertThat(((ApplicationException) e).getCode())
                                        .isEqualTo(AdminErrorCode.ADMIN_REFRESH_TOKEN_INVALID));
    }

    @Test
    @DisplayName("refreshToken - ADMIN이 아닌 role이면 예외를 던진다")
    void refreshToken_nonAdminRole_throwsException() {
        // given
        Claims claims = mock(Claims.class);
        given(claims.get("role", String.class)).willReturn("MEMBER");

        given(jwtTokenUtil.verifyRefreshToken("member-refresh-token")).willReturn(claims);

        // when & then
        assertThatThrownBy(() -> adminService.refreshToken("member-refresh-token"))
                .isInstanceOf(ApplicationException.class)
                .satisfies(
                        e ->
                                assertThat(((ApplicationException) e).getCode())
                                        .isEqualTo(AdminErrorCode.ADMIN_REFRESH_TOKEN_INVALID));
    }

    @Test
    @DisplayName("refreshToken - 만료/변조된 토큰이면 예외를 던진다")
    void refreshToken_expiredToken_throwsException() {
        // given
        given(jwtTokenUtil.verifyRefreshToken("expired-token"))
                .willThrow(new JwtException("만료된 토큰"));

        // when & then
        assertThatThrownBy(() -> adminService.refreshToken("expired-token"))
                .isInstanceOf(ApplicationException.class)
                .satisfies(
                        e ->
                                assertThat(((ApplicationException) e).getCode())
                                        .isEqualTo(AdminErrorCode.ADMIN_REFRESH_TOKEN_INVALID));
    }

    @Test
    @DisplayName("refreshToken - subject가 숫자가 아니면 예외를 던진다")
    void refreshToken_invalidSubject_throwsException() {
        // given
        Claims claims = mock(Claims.class);
        given(claims.get("role", String.class)).willReturn("ADMIN");
        given(claims.getSubject()).willReturn("not-a-number");

        given(jwtTokenUtil.verifyRefreshToken("bad-subject-token")).willReturn(claims);

        // when & then
        assertThatThrownBy(() -> adminService.refreshToken("bad-subject-token"))
                .isInstanceOf(ApplicationException.class)
                .satisfies(
                        e ->
                                assertThat(((ApplicationException) e).getCode())
                                        .isEqualTo(AdminErrorCode.ADMIN_REFRESH_TOKEN_INVALID));
    }

    @Test
    @DisplayName("refreshToken - DB에 존재하지 않는 관리자이면 예외를 던진다")
    void refreshToken_adminNotFound_throwsException() {
        // given
        Claims claims = mock(Claims.class);
        given(claims.get("role", String.class)).willReturn("ADMIN");
        given(claims.getSubject()).willReturn("999");

        given(jwtTokenUtil.verifyRefreshToken("deleted-admin-token")).willReturn(claims);
        given(adminRepository.existsById(999L)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> adminService.refreshToken("deleted-admin-token"))
                .isInstanceOf(ApplicationException.class)
                .satisfies(
                        e ->
                                assertThat(((ApplicationException) e).getCode())
                                        .isEqualTo(AdminErrorCode.ADMIN_REFRESH_TOKEN_INVALID));
    }

    @Test
    @DisplayName("refreshToken - role이 유효하지 않은 문자열이면 예외를 던진다")
    void refreshToken_invalidRoleString_throwsException() {
        // given
        Claims claims = mock(Claims.class);
        given(claims.get("role", String.class)).willReturn("INVALID_ROLE");

        given(jwtTokenUtil.verifyRefreshToken("invalid-role-token")).willReturn(claims);

        // when & then
        assertThatThrownBy(() -> adminService.refreshToken("invalid-role-token"))
                .isInstanceOf(ApplicationException.class)
                .satisfies(
                        e ->
                                assertThat(((ApplicationException) e).getCode())
                                        .isEqualTo(AdminErrorCode.ADMIN_REFRESH_TOKEN_INVALID));
    }
}
