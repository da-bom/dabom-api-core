package com.project.domain.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.project.domain.admin.entity.Admin;
import com.project.domain.admin.repository.AdminRepository;
import com.project.domain.customer.dto.response.SignUpResponse;
import com.project.domain.customer.enums.RoleType;
import com.project.global.auth.JwtTokenUtil;
import com.project.global.auth.PasswordHash;
import com.project.global.auth.SignInResult;
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
    @DisplayName("signIn - 올바른 자격증명이면 토큰과 ADMIN 역할을 반환한다")
    void signIn_validCredentials_returnsTokensAndAdminRole() {
        // given
        Admin admin =
                Admin.builder()
                        .id(1L)
                        .email("admin@test.com")
                        .name("ADMIN")
                        .passwordHash("hashed-pw")
                        .build();

        given(adminRepository.findByEmail("admin@test.com")).willReturn(Optional.of(admin));
        given(jwtTokenUtil.createToken(1L, RoleType.ADMIN)).willReturn("access-token");
        given(jwtTokenUtil.createRefreshToken(1L, RoleType.ADMIN)).willReturn("refresh-token");

        try (MockedStatic<PasswordHash> passwordHash = mockStatic(PasswordHash.class)) {
            passwordHash.when(() -> PasswordHash.matches("raw-pw", "hashed-pw")).thenReturn(true);

            // when
            SignInResult result = adminService.signIn("admin@test.com", "raw-pw");

            // then
            assertThat(result.accessToken()).isEqualTo("access-token");
            assertThat(result.refreshToken()).isEqualTo("refresh-token");
            assertThat(result.role()).isEqualTo(RoleType.ADMIN);
        }
    }

    @Test
    @DisplayName("signIn - 존재하지 않는 이메일이면 예외를 던진다")
    void signIn_adminNotFound_throwsException() {
        // given
        given(adminRepository.findByEmail("unknown@test.com")).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> adminService.signIn("unknown@test.com", "raw-pw"))
                .isInstanceOf(ApplicationException.class)
                .satisfies(
                        e ->
                                assertThat(((ApplicationException) e).getCode())
                                        .isEqualTo(AdminErrorCode.ADMIN_SIGN_IN_FAILED));
    }

    @Test
    @DisplayName("signIn - 비밀번호가 틀리면 예외를 던진다")
    void signIn_wrongPassword_throwsException() {
        // given
        Admin admin =
                Admin.builder()
                        .id(1L)
                        .email("admin@test.com")
                        .name("ADMIN")
                        .passwordHash("hashed-pw")
                        .build();

        given(adminRepository.findByEmail("admin@test.com")).willReturn(Optional.of(admin));

        try (MockedStatic<PasswordHash> passwordHash = mockStatic(PasswordHash.class)) {
            passwordHash
                    .when(() -> PasswordHash.matches("wrong-pw", "hashed-pw"))
                    .thenReturn(false);

            // when & then
            assertThatThrownBy(() -> adminService.signIn("admin@test.com", "wrong-pw"))
                    .isInstanceOf(ApplicationException.class)
                    .satisfies(
                            e ->
                                    assertThat(((ApplicationException) e).getCode())
                                            .isEqualTo(AdminErrorCode.ADMIN_SIGN_IN_FAILED));
        }
    }

    @Test
    @DisplayName("signUp - 정상 요청이면 관리자를 저장하고 ID를 반환한다")
    void signUp_validRequest_returnsAdminId() {
        // given
        given(adminRepository.save(any(Admin.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        try (MockedStatic<PasswordHash> passwordHash = mockStatic(PasswordHash.class)) {
            passwordHash.when(() -> PasswordHash.hash("raw-pw")).thenReturn("hashed-pw");

            // when
            SignUpResponse result = adminService.signUp("admin@test.com", "raw-pw");

            // then
            assertThat(result).isNotNull();
        }
    }

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
