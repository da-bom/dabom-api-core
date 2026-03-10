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

import com.project.domain.customer.enums.RoleType;
import com.project.domain.customer.repository.CustomerRepository;
import com.project.domain.family.repository.FamilyMemberRepository;
import com.project.global.auth.JwtTokenUtil;
import com.project.global.auth.TokenRefreshResult;
import com.project.global.exception.ApplicationException;
import com.project.global.exception.code.CustomerErrorCode;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

@ExtendWith(MockitoExtension.class)
class CustomerServiceImplTest {

    @Mock private CustomerRepository customerRepository;
    @Mock private FamilyMemberRepository familyMemberRepository;
    @Mock private JwtTokenUtil jwtTokenUtil;

    @InjectMocks private CustomerServiceImpl customerService;

    @Test
    @DisplayName("refreshToken - OWNER role의 유효한 refresh token이면 새 토큰 쌍을 반환한다")
    void refreshToken_validOwnerRefreshToken_returnsNewTokenPair() {
        // given
        Claims claims = mock(Claims.class);
        given(claims.get("role", String.class)).willReturn("OWNER");
        given(claims.getSubject()).willReturn("10");

        given(jwtTokenUtil.verifyRefreshToken("owner-refresh-token")).willReturn(claims);
        given(customerRepository.existsById(10L)).willReturn(true);
        given(familyMemberRepository.findRoleById(10L)).willReturn(RoleType.OWNER);
        given(jwtTokenUtil.reissueTokens(10L, RoleType.OWNER))
                .willReturn(new TokenRefreshResult("new-access", "new-refresh", 1800L));

        // when
        TokenRefreshResult result = customerService.refreshToken("owner-refresh-token");

        // then
        assertThat(result.accessToken()).isEqualTo("new-access");
        assertThat(result.refreshToken()).isEqualTo("new-refresh");
        assertThat(result.expiresIn()).isEqualTo(1800L);
    }

    @Test
    @DisplayName("refreshToken - MEMBER role의 유효한 refresh token이면 새 토큰 쌍을 반환한다")
    void refreshToken_validMemberRefreshToken_returnsNewTokenPair() {
        // given
        Claims claims = mock(Claims.class);
        given(claims.get("role", String.class)).willReturn("MEMBER");
        given(claims.getSubject()).willReturn("20");

        given(jwtTokenUtil.verifyRefreshToken("member-refresh-token")).willReturn(claims);
        given(customerRepository.existsById(20L)).willReturn(true);
        given(familyMemberRepository.findRoleById(20L)).willReturn(RoleType.MEMBER);
        given(jwtTokenUtil.reissueTokens(20L, RoleType.MEMBER))
                .willReturn(new TokenRefreshResult("new-access", "new-refresh", 1800L));

        // when
        TokenRefreshResult result = customerService.refreshToken("member-refresh-token");

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
        assertThatThrownBy(() -> customerService.refreshToken("access-token"))
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
        given(claims.get("role", String.class)).willReturn("ADMIN");

        given(jwtTokenUtil.verifyRefreshToken("admin-refresh-token")).willReturn(claims);

        // when & then
        assertThatThrownBy(() -> customerService.refreshToken("admin-refresh-token"))
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
        given(jwtTokenUtil.verifyRefreshToken("expired-token"))
                .willThrow(new JwtException("만료된 토큰"));

        // when & then
        assertThatThrownBy(() -> customerService.refreshToken("expired-token"))
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
        given(claims.get("role", String.class)).willReturn("OWNER");
        given(claims.getSubject()).willReturn("not-a-number");

        given(jwtTokenUtil.verifyRefreshToken("bad-subject-token")).willReturn(claims);

        // when & then
        assertThatThrownBy(() -> customerService.refreshToken("bad-subject-token"))
                .isInstanceOf(ApplicationException.class)
                .satisfies(
                        e ->
                                assertThat(((ApplicationException) e).getCode())
                                        .isEqualTo(
                                                CustomerErrorCode.CUSTOMER_REFRESH_TOKEN_INVALID));
    }

    @Test
    @DisplayName("refreshToken - DB에 존재하지 않는 사용자이면 예외를 던진다")
    void refreshToken_customerNotFound_throwsException() {
        // given
        Claims claims = mock(Claims.class);
        given(claims.get("role", String.class)).willReturn("MEMBER");
        given(claims.getSubject()).willReturn("999");

        given(jwtTokenUtil.verifyRefreshToken("deleted-customer-token")).willReturn(claims);
        given(customerRepository.existsById(999L)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> customerService.refreshToken("deleted-customer-token"))
                .isInstanceOf(ApplicationException.class)
                .satisfies(
                        e ->
                                assertThat(((ApplicationException) e).getCode())
                                        .isEqualTo(
                                                CustomerErrorCode.CUSTOMER_REFRESH_TOKEN_INVALID));
    }

    @Test
    @DisplayName("refreshToken - role이 유효하지 않은 문자열이면 예외를 던진다")
    void refreshToken_invalidRoleString_throwsException() {
        // given
        Claims claims = mock(Claims.class);
        given(claims.get("role", String.class)).willReturn("INVALID_ROLE");

        given(jwtTokenUtil.verifyRefreshToken("invalid-role-token")).willReturn(claims);

        // when & then
        assertThatThrownBy(() -> customerService.refreshToken("invalid-role-token"))
                .isInstanceOf(ApplicationException.class)
                .satisfies(
                        e ->
                                assertThat(((ApplicationException) e).getCode())
                                        .isEqualTo(
                                                CustomerErrorCode.CUSTOMER_REFRESH_TOKEN_INVALID));
    }
}
