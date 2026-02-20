package com.project.domain.policy.support;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.project.common.TestFixtureBuilder;
import com.project.common.fixtures.CustomerFixtures;
import com.project.common.fixtures.FamilyMemberFixtures;
import com.project.common.fixtures.PolicyAssignmentFixtures;
import com.project.common.fixtures.PolicyFixtures;
import com.project.domain.customer.entity.Customer;
import com.project.domain.customer.enums.RoleType;
import com.project.domain.family.entity.FamilyMember;
import com.project.domain.policy.entity.Policy;
import com.project.domain.policy.entity.PolicyAssignment;
import com.project.domain.policy.enums.PolicyType;

@Component
public class PolicyApiTestSupport {

    private final TestFixtureBuilder testFixtureBuilder;

    public PolicyApiTestSupport(TestFixtureBuilder testFixtureBuilder) {
        this.testFixtureBuilder = testFixtureBuilder;
    }

    public FamilyContext buildFamilyContext(Long familyId) {
        Customer owner = testFixtureBuilder.buildCustomer(CustomerFixtures.dad());
        Customer member1 = testFixtureBuilder.buildCustomer(CustomerFixtures.mom());
        Customer member2 = testFixtureBuilder.buildCustomer(CustomerFixtures.kid());

        FamilyMember ownerMember =
                testFixtureBuilder.buildFamilyMember(
                        FamilyMemberFixtures.owner(familyId, owner.getId()));
        FamilyMember familyMember1 =
                testFixtureBuilder.buildFamilyMember(
                        FamilyMemberFixtures.member(familyId, member1.getId()));
        FamilyMember familyMember2 =
                testFixtureBuilder.buildFamilyMember(
                        FamilyMemberFixtures.member(familyId, member2.getId()));

        return new FamilyContext(
                owner, member1, member2, ownerMember, familyMember1, familyMember2);
    }

    public PolicyContext buildPolicyContext(FamilyContext familyContext) {
        Policy policy =
                testFixtureBuilder.buildPolicy(
                        PolicyFixtures.monthlyLimitPolicy(Map.of("limitBytes", 1000), true));

        PolicyAssignment assignment1 =
                PolicyAssignmentFixtures.assignment(
                        policy.getId(),
                        familyContext.ownerMember().getFamilyId(),
                        familyContext.member1().getId(),
                        "{\"limitBytes\":1000}",
                        true);

        PolicyAssignment assignment2 =
                PolicyAssignmentFixtures.assignment(
                        policy.getId(),
                        familyContext.ownerMember().getFamilyId(),
                        familyContext.member2().getId(),
                        "{\"limitBytes\":1000}",
                        true);

        List<PolicyAssignment> assignments =
                testFixtureBuilder.buildPolicyAssignments(List.of(assignment1, assignment2));

        return new PolicyContext(policy, assignments);
    }

    public Policy buildPolicy(String name, Map<String, Object> defaultRules, boolean isActive) {
        return testFixtureBuilder.buildPolicy(
                PolicyFixtures.monthlyLimitPolicy(name, defaultRules, isActive));
    }

    public Policy buildDetailPolicy(
            String name,
            String description,
            RoleType requiredRole,
            PolicyType policyType,
            Map<String, Object> defaultRules,
            boolean isSystem,
            boolean isActive) {
        return testFixtureBuilder.buildPolicy(
                PolicyFixtures.makePolicy(
                        name,
                        description,
                        defaultRules,
                        requiredRole,
                        policyType,
                        isSystem,
                        isActive));
    }

    public record FamilyContext(
            Customer owner,
            Customer member1,
            Customer member2,
            FamilyMember ownerMember,
            FamilyMember familyMember1,
            FamilyMember familyMember2) {}

    public record PolicyContext(Policy policy, List<PolicyAssignment> assignments) {}
}
