package com.project.domain.policy.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.domain.customer.enums.RoleType;
import com.project.domain.policy.entity.PolicyAssignment;
import com.project.domain.policy.enums.PolicyType;
import com.project.domain.policy.repository.PolicyAssignmentRepository;
import com.project.domain.policy.support.PolicyApiTestSupport;
import com.project.global.auth.JwtTokenUtil;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@EmbeddedKafka( //
        partitions = 1,
        topics = {"usage-events"},
        brokerProperties = {"listeners=PLAINTEXT://localhost:0", "port=0"})
class PolicyControllerIntegrationTest {

    private static final String ADMIN_TOKEN = "ADMIN";

    @Autowired private MockMvc mockMvc;

    @Autowired private ObjectMapper objectMapper;

    @Autowired private PolicyApiTestSupport policyApiTestSupport;

    @Autowired private PolicyAssignmentRepository policyAssignmentRepository;
    @Autowired private EntityManager entityManager;

    @MockitoBean private KafkaTemplate<String, Object> kafkaTemplate;
    @MockitoBean private JwtTokenUtil jwtTokenUtil;

    @Test
    @DisplayName("GET /policies - page 메타정보와 정책들을 반환합니다.")
    void getPolicyListReturnsPagedResult() throws Exception {
        policyApiTestSupport.buildPolicy("list-policy-1", Map.of("limitBytes", 1000), true);
        policyApiTestSupport.buildPolicy("list-policy-2", Map.of("limitBytes", 2000), false);
        given(jwtTokenUtil.getRole(ADMIN_TOKEN)).willReturn(RoleType.ADMIN);

        MvcResult mvcResult =
                mockMvc.perform(get("/policies").header("Authorization", "Bearer " + ADMIN_TOKEN))
                        .andExpect(status().isOk())
                        .andReturn();

        JsonNode data =
                objectMapper.readTree(mvcResult.getResponse().getContentAsString()).path("data");
        assertThat(data.path("page").asInt()).isEqualTo(1);
        assertThat(data.path("size").asInt()).isEqualTo(10);
        assertThat(data.path("totalElements").asInt()).isEqualTo(2);
        assertThat(data.path("totalPages").asInt()).isEqualTo(1);
        assertThat(data.path("policies").size()).isEqualTo(2);

        List<String> policyNames =
                data.path("policies").findValuesAsText("name").stream().sorted().toList();
        assertThat(policyNames).containsExactly("list-policy-1", "list-policy-2");
    }

    @Test
    @DisplayName("GET /policies/{policyId} - 정책 상세 정보를 반환한다")
    void getPolicyDetailReturnsPolicyDetail() throws Exception {
        var policy =
                policyApiTestSupport.buildDetailPolicy(
                        "detail-policy",
                        "detail description",
                        RoleType.OWNER,
                        PolicyType.MONTHLY_LIMIT,
                        Map.of("limitBytes", 4096),
                        true,
                        true);

        given(jwtTokenUtil.getRole(ADMIN_TOKEN)).willReturn(RoleType.ADMIN);

        MvcResult mvcResult =
                mockMvc.perform(
                                get("/policies/{policyId}", policy.getId())
                                        .header("Authorization", "Bearer " + ADMIN_TOKEN))
                        .andExpect(status().isOk())
                        .andReturn();

        JsonNode data =
                objectMapper.readTree(mvcResult.getResponse().getContentAsString()).path("data");

        assertThat(data.path("id").asLong()).isEqualTo(policy.getId());
        assertThat(data.path("name").asText()).isEqualTo("detail-policy");
        assertThat(data.path("description").asText()).isEqualTo("detail description");
        assertThat(data.path("requiredRole").asText()).isEqualTo("OWNER");
        assertThat(data.path("policyType").asText()).isEqualTo("MONTHLY_LIMIT");
        assertThat(data.path("defaultRules").path("limitBytes").asInt()).isEqualTo(4096);
        assertThat(data.path("isSystem").asBoolean()).isTrue();
        assertThat(data.path("isActive").asBoolean()).isTrue();
    }

    @Test
    @DisplayName("PUT /policies/{policyId} - overWrite=true 가족에 적용된 정책을 즉시 수정한다.")
    void updatePolicyOverwriteTrueUpdatesExistingAssignments() throws Exception {
        PolicyApiTestSupport.FamilyContext familyContext =
                policyApiTestSupport.buildFamilyContext(100L);
        PolicyApiTestSupport.PolicyContext policyContext =
                policyApiTestSupport.buildPolicyContext(familyContext);

        given(jwtTokenUtil.getRole(ADMIN_TOKEN)).willReturn(RoleType.ADMIN);

        UpdatePolicyRequest request =
                new UpdatePolicyRequest(
                        "updated description",
                        RoleType.OWNER,
                        "MONTHLY_LIMIT",
                        Map.of("limitBytes", 5000),
                        false,
                        true);

        mockMvc.perform(
                        put("/policies/{policyId}", policyContext.policy().getId())
                                .header("Authorization", "Bearer " + ADMIN_TOKEN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        entityManager.flush();
        entityManager.clear();

        for (PolicyAssignment assignment :
                policyAssignmentRepository.findAllByPolicyId(policyContext.policy().getId())) {
            JsonNode rulesNode = parseRulesNode(assignment.getRules());
            assertThat(rulesNode.path("limitBytes").asInt()).isEqualTo(5000);
            assertThat(assignment.isActive()).isFalse();
        }
    }

    @Test
    @DisplayName("PUT /policies/{policyId} - overWrite=false 부여된 정책들은 놔두고 새로 생성되는 정책에 경우 적용")
    void updatePolicyOverwriteFalseKeepsExistingAssignments() throws Exception {
        PolicyApiTestSupport.FamilyContext familyContext =
                policyApiTestSupport.buildFamilyContext(101L);
        PolicyApiTestSupport.PolicyContext policyContext =
                policyApiTestSupport.buildPolicyContext(familyContext);

        given(jwtTokenUtil.getRole(ADMIN_TOKEN)).willReturn(RoleType.ADMIN);

        UpdatePolicyRequest request =
                new UpdatePolicyRequest(
                        "updated description",
                        RoleType.OWNER,
                        "MONTHLY_LIMIT",
                        Map.of("limitBytes", 5000),
                        false,
                        false);

        mockMvc.perform(
                        put("/policies/{policyId}", policyContext.policy().getId())
                                .header("Authorization", "Bearer " + ADMIN_TOKEN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        for (PolicyAssignment assignment :
                policyAssignmentRepository.findAllByPolicyId(policyContext.policy().getId())) {
            JsonNode rulesNode = parseRulesNode(assignment.getRules());
            assertThat(rulesNode.path("limitBytes").asInt()).isEqualTo(1000);
            assertThat(assignment.isActive()).isTrue();
        }
    }

    private JsonNode parseRulesNode(String rawRules) throws Exception {
        JsonNode parsed = objectMapper.readTree(rawRules);
        if (parsed.isTextual()) {
            return objectMapper.readTree(parsed.asText());
        }
        return parsed;
    }

    private record UpdatePolicyRequest(
            String description,
            RoleType requiredRole,
            String policyType,
            Map<String, Object> defaultRules,
            boolean isActive,
            boolean overWrite) {}
}
