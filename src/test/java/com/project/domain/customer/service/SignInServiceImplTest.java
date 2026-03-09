package com.project.domain.customer.service;

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

import com.project.domain.customer.dto.response.CustomerRefreshResponse;
import com.project.domain.customer.enums.RoleType;
import com.project.domain.customer.repository.CustomerRepository;
import com.project.domain.family.repository.FamilyMemberRepository;
import com.project.global.auth.JwtTokenUtil;
import com.project.global.exception.ApplicationException;
import com.project.global.exception.code.CustomerErrorCode;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

@ExtendWith(MockitoExtension.class)
class SignInServiceImplTest {

    @Mock private CustomerRepository customerRepository;
    @Mock private FamilyMemberRepository familyMemberRepository;
    @Mock private JwtTokenUtil jwtTokenUtil;

    @InjectMocks private SignInServiceImpl signInService;

    @Test
    @DisplayName("refreshToken - OWNER role의 유효한 refresh token이면 새 토큰 쌍을 반환한다")
    void refreshToken_validOwnerRefreshToken_returnsNewTokenPair() {
        // given
        Claims claims = mock(Claims.class);
        given(claims.get("type", String.class)).willReturn("refresh");
        given(claims.get("role", String.class)).willReturn("OWNER");
        given(claims.getSubject()).willReturn("10");

        given(jwtTokenUtil.verify("owner-refresh-token")).willReturn(claims);
        given(jwtTokenUtil.createToken(10L, RoleType.OWNER)).willReturn("new-access");
        given(jwtTokenUtil.createRefreshToken(10L, RoleType.OWNER)).willReturn("new-refresh");
        given(jwtTokenUtil.getRefreshTokenExpirationMillis()).willReturn(1800000L);

        // when
        CustomerRefreshResponse response = signInService.refreshToken("owner-refresh-token");

        // then
        assertThat(response.accessToken()).isEqualTo("new-access");
        assertThat(response.refreshToken()).isEqualTo("new-refresh");
        assertThat(response.expiresIn()).isEqualTo(1800L);
    }

    @Test
    @DisplayName("refreshToken - MEMBER role의 유효한 refresh token이면 새 토큰 쌍을 반환한다")
    void refreshToken_validMemberRefreshToken_returnsNewTokenPair() {
        // given
        Claims claims = mock(Claims.class);
        given(claims.get("type", String.class)).willReturn("refresh");
        given(claims.get("role", String.class)).willReturn("MEMBER");
        given(claims.getSubject()).willReturn("20");

        given(jwtTokenUtil.verify("member-refresh-token")).willReturn(claims);
        given(jwtTokenUtil.createToken(20L, RoleType.MEMBER)).willReturn("new-access");
        given(jwtTokenUtil.createRefreshToken(20L, RoleType.MEMBER)).willReturn("new-refresh");
        given(jwtTokenUtil.getRefreshTokenExpirationMillis()).willReturn(1800000L);

        // when
        CustomerRefreshResponse response = signInService.refreshToken("member-refresh-token");

        // then
        assertThat(response.accessToken()).isEqualTo("new-access");
        assertThat(response.refreshToken()).isEqualTo("new-refresh");
        assertThat(response.expiresIn()).isEqualTo(1800L);
    }

    @Test
    @DisplayName("refreshToken - access token을 사용하면 예외를 던진다")
    void refreshToken_accessToken_throwsException() {
        // given
        Claims claims = mock(Claims.class);
        given(claims.get("type", String.class)).willReturn("access");

        given(jwtTokenUtil.verify("access-token")).willReturn(claims);

        // when & then
        assertThatThrownBy(() -> signInService.refreshToken("access-token"))
                .isInstanceOf(ApplicationException.class)
                .satisfies(
                        e ->
                                assertThat(((ApplicationException) e).getCode())
                                        .isEqualTo(
                                                CustomerErrorCode.CUSTOMER_REFRESH_TOKEN_INVALID));
    }

    @Test
    @DisplayName("refreshToken - ADMIN role이면 예외를 던진다")
    void refreshToken_adminRole_throwsException() {
        // given
        Claims claims = mock(Claims.class);
        given(claims.get("type", String.class)).willReturn("refresh");
        given(claims.get("role", String.class)).willReturn("ADMIN");

        given(jwtTokenUtil.verify("admin-refresh-token")).willReturn(claims);

        // when & then
        assertThatThrownBy(() -> signInService.refreshToken("admin-refresh-token"))
                .isInstanceOf(ApplicationException.class)
                .satisfies(
                        e ->
                                assertThat(((ApplicationException) e).getCode())
                                        .isEqualTo(
                                                CustomerErrorCode.CUSTOMER_REFRESH_TOKEN_INVALID));
    }

    @Test
    @DisplayName("refreshToken - 만료/변조된 토큰이면 예외를 던진다")
    void refreshToken_expiredToken_throwsException() {
        // given
        given(jwtTokenUtil.verify("expired-token")).willThrow(new JwtException("만료된 토큰"));

        // when & then
        assertThatThrownBy(() -> signInService.refreshToken("expired-token"))
                .isInstanceOf(ApplicationException.class)
                .satisfies(
                        e ->
                                assertThat(((ApplicationException) e).getCode())
                                        .isEqualTo(
                                                CustomerErrorCode.CUSTOMER_REFRESH_TOKEN_INVALID));
    }

    @Test
    @DisplayName("refreshToken - subject가 숫자가 아니면 예외를 던진다")
    void refreshToken_invalidSubject_throwsException() {
        // given
        Claims claims = mock(Claims.class);
        given(claims.get("type", String.class)).willReturn("refresh");
        given(claims.get("role", String.class)).willReturn("OWNER");
        given(claims.getSubject()).willReturn("not-a-number");

        given(jwtTokenUtil.verify("bad-subject-token")).willReturn(claims);

        // when & then
        assertThatThrownBy(() -> signInService.refreshToken("bad-subject-token"))
                .isInstanceOf(ApplicationException.class)
                .satisfies(
                        e ->
                                assertThat(((ApplicationException) e).getCode())
                                        .isEqualTo(
                                                CustomerErrorCode.CUSTOMER_REFRESH_TOKEN_INVALID));
    }
}
