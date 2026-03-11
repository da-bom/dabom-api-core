package com.project.domain.customer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.domain.customer.entity.Customer;
import com.project.domain.customer.entity.CustomerQuota;
import com.project.domain.customer.enums.RoleType;
import com.project.domain.customer.model.MyPageInfo;
import com.project.domain.customer.repository.CustomerQuotaRepository;
import com.project.domain.customer.repository.CustomerRepository;
import com.project.domain.family.entity.Family;
import com.project.domain.family.repository.FamilyMemberRepository;
import com.project.domain.family.repository.FamilyRepository;
import com.project.domain.policy.entity.PolicyAssignment;
import com.project.domain.policy.enums.PolicyType;
import com.project.domain.policy.repository.PolicyAssignmentRepository;
import com.project.global.auth.JwtTokenUtil;
import com.project.global.auth.TokenRefreshResult;
import com.project.global.auth.model.AuthContext;
import com.project.global.exception.ApplicationException;
import com.project.global.exception.code.CustomerErrorCode;
import com.project.global.exception.code.PolicyErrorCode;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

@ExtendWith(MockitoExtension.class)
class CustomerServiceImplTest {

    @Mock private CustomerRepository customerRepository;
    @Mock private FamilyMemberRepository familyMemberRepository;
    @Mock private FamilyRepository familyRepository;
    @Mock private CustomerQuotaRepository customerQuotaRepository;
    @Mock private PolicyAssignmentRepository policyAssignmentRepository;
    @Mock private JwtTokenUtil jwtTokenUtil;
    @Mock private ObjectMapper objectMapper;

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

    @Test
    @DisplayName("getMyPageInfo - 마이페이지 정보를 반환한다")
    void getMyPageInfo_returnsMyPageInfo() throws Exception {
        // given
        AuthContext authContext = new AuthContext(10L, 100L, RoleType.MEMBER);
        Customer customer = new Customer("01012345678", "encoded-password", "철수");
        Family family =
                Family.builder()
                        .id(100L)
                        .name("테스트 가족")
                        .createdById(1L)
                        .totalQuotaBytes(10_000L)
                        .usedBytes(4_000L)
                        .currentMonth(java.time.LocalDate.of(2026, 3, 1))
                        .build();
        CustomerQuota customerQuota =
                CustomerQuota.builder()
                        .customerId(10L)
                        .familyId(100L)
                        .monthlyLimitBytes(3_000L)
                        .monthlyUsedBytes(1_200L)
                        .currentMonth(java.time.LocalDate.of(2026, 3, 1))
                        .isBlocked(true)
                        .blockReason("TIME_POLICY")
                        .build();
        PolicyAssignment policyAssignment =
                PolicyAssignment.builder()
                        .id(1L)
                        .policyId(20L)
                        .familyId(100L)
                        .targetCustomerId(10L)
                        .rules("{\"startTime\":\"22:00\",\"endTime\":\"07:00\"}")
                        .isActive(true)
                        .build();
        JsonNode timeBlockNode = new ObjectMapper().readTree(policyAssignment.getRules());

        given(customerRepository.findById(10L)).willReturn(Optional.of(customer));
        given(familyRepository.findById(100L)).willReturn(Optional.of(family));
        given(customerQuotaRepository.findById(10L)).willReturn(Optional.of(customerQuota));
        given(policyAssignmentRepository.findByTargetAndType(100L, 10L, PolicyType.TIME_BLOCK))
                .willReturn(Optional.of(policyAssignment));
        given(objectMapper.readTree(policyAssignment.getRules())).willReturn(timeBlockNode);

        // when
        MyPageInfo result = customerService.getMyPageInfo(authContext);

        // then
        assertThat(result.name()).isEqualTo("철수");
        assertThat(result.familyName()).isEqualTo("테스트 가족");
        assertThat(result.isBlocked()).isTrue();
        assertThat(result.blockReason()).isEqualTo("TIME_POLICY");
        assertThat(result.monthlyLimitBytes()).isEqualTo(3_000L);
        assertThat(result.monthlyUsedBytes()).isEqualTo(1_200L);
        assertThat(result.timeBlock().path("startTime").asText()).isEqualTo("22:00");
        assertThat(result.timeBlock().path("endTime").asText()).isEqualTo("07:00");
    }

    @Test
    @DisplayName("getMyPageInfo - TIME_BLOCK 정책이 없으면 예외를 던진다")
    void getMyPageInfo_policyAssignmentNotFound_throwsException() {
        // given
        AuthContext authContext = new AuthContext(10L, 100L, RoleType.MEMBER);
        Customer customer = new Customer("01012345678", "encoded-password", "철수");
        Family family =
                Family.builder()
                        .id(100L)
                        .name("테스트 가족")
                        .createdById(1L)
                        .totalQuotaBytes(10_000L)
                        .usedBytes(4_000L)
                        .currentMonth(java.time.LocalDate.of(2026, 3, 1))
                        .build();
        CustomerQuota customerQuota =
                CustomerQuota.builder()
                        .customerId(10L)
                        .familyId(100L)
                        .monthlyLimitBytes(3_000L)
                        .monthlyUsedBytes(1_200L)
                        .currentMonth(java.time.LocalDate.of(2026, 3, 1))
                        .isBlocked(false)
                        .blockReason(null)
                        .build();

        given(customerRepository.findById(10L)).willReturn(Optional.of(customer));
        given(familyRepository.findById(100L)).willReturn(Optional.of(family));
        given(customerQuotaRepository.findById(10L)).willReturn(Optional.of(customerQuota));
        given(policyAssignmentRepository.findByTargetAndType(100L, 10L, PolicyType.TIME_BLOCK))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> customerService.getMyPageInfo(authContext))
                .isInstanceOf(ApplicationException.class)
                .satisfies(
                        e ->
                                assertThat(((ApplicationException) e).getCode())
                                        .isEqualTo(PolicyErrorCode.POLICY_ASSIGNMENT_NOT_FOUND));
    }

    @Test
    @DisplayName("getMyPageInfo - TIME_BLOCK rules 파싱에 실패하면 예외를 던진다")
    void getMyPageInfo_invalidTimeBlockRules_throwsException() throws Exception {
        // given
        AuthContext authContext = new AuthContext(10L, 100L, RoleType.MEMBER);
        Customer customer = new Customer("01012345678", "encoded-password", "철수");
        Family family =
                Family.builder()
                        .id(100L)
                        .name("테스트 가족")
                        .createdById(1L)
                        .totalQuotaBytes(10_000L)
                        .usedBytes(4_000L)
                        .currentMonth(java.time.LocalDate.of(2026, 3, 1))
                        .build();
        CustomerQuota customerQuota =
                CustomerQuota.builder()
                        .customerId(10L)
                        .familyId(100L)
                        .monthlyLimitBytes(3_000L)
                        .monthlyUsedBytes(1_200L)
                        .currentMonth(java.time.LocalDate.of(2026, 3, 1))
                        .isBlocked(true)
                        .blockReason("TIME_POLICY")
                        .build();
        PolicyAssignment policyAssignment =
                PolicyAssignment.builder()
                        .id(1L)
                        .policyId(20L)
                        .familyId(100L)
                        .targetCustomerId(10L)
                        .rules("invalid-json")
                        .isActive(true)
                        .build();

        given(customerRepository.findById(10L)).willReturn(Optional.of(customer));
        given(familyRepository.findById(100L)).willReturn(Optional.of(family));
        given(customerQuotaRepository.findById(10L)).willReturn(Optional.of(customerQuota));
        given(policyAssignmentRepository.findByTargetAndType(100L, 10L, PolicyType.TIME_BLOCK))
                .willReturn(Optional.of(policyAssignment));
        given(objectMapper.readTree("invalid-json"))
                .willThrow(new JsonProcessingException("bad json") {});

        // when & then
        assertThatThrownBy(() -> customerService.getMyPageInfo(authContext))
                .isInstanceOf(ApplicationException.class)
                .satisfies(
                        e ->
                                assertThat(((ApplicationException) e).getCode())
                                        .isEqualTo(
                                                PolicyErrorCode.POLICY_RULES_SERIALIZATION_FAILED));
    }
}
