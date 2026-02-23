package com.project.domain.family.support;

import org.springframework.stereotype.Component;

import com.project.common.TestFixtureBuilder;
import com.project.common.fixtures.CustomerFixtures;
import com.project.common.fixtures.CustomerQuotaFixtures;
import com.project.common.fixtures.FamilyFixtures;
import com.project.common.fixtures.FamilyMemberFixtures;
import com.project.domain.customer.entity.Customer;
import com.project.domain.family.entity.Family;
import com.project.domain.family.entity.FamilyMember;

@Component
public class FamilyApiTestSupport {

    private final TestFixtureBuilder testFixtureBuilder;

    public FamilyApiTestSupport(TestFixtureBuilder testFixtureBuilder) {
        this.testFixtureBuilder = testFixtureBuilder;
    }

    public FamilyContext buildFamilyContext() {
        Customer dad = testFixtureBuilder.buildCustomer(CustomerFixtures.dad());
        Customer mom = testFixtureBuilder.buildCustomer(CustomerFixtures.mom());
        Customer kid = testFixtureBuilder.buildCustomer(CustomerFixtures.kid());

        Family family = testFixtureBuilder.buildFamily(FamilyFixtures.family(dad.getId()));

        FamilyMember dadMember =
                testFixtureBuilder.buildFamilyMember(
                        FamilyMemberFixtures.owner(family.getId(), dad.getId()));
        FamilyMember momMember =
                testFixtureBuilder.buildFamilyMember(
                        FamilyMemberFixtures.owner(family.getId(), mom.getId()));
        FamilyMember kidMember =
                testFixtureBuilder.buildFamilyMember(
                        FamilyMemberFixtures.member(family.getId(), kid.getId()));

        return new FamilyContext(family, dad, mom, kid, dadMember, momMember, kidMember);
    }

    public void buildQuotas(FamilyContext familyContext, long dadUsed, long momUsed, long kidUsed) {
        testFixtureBuilder.buildCustomerQuota(
                CustomerQuotaFixtures.quota(
                        familyContext.family().getId(), familyContext.dad().getId(), dadUsed));
        testFixtureBuilder.buildCustomerQuota(
                CustomerQuotaFixtures.quota(
                        familyContext.family().getId(), familyContext.mom().getId(), momUsed));
        testFixtureBuilder.buildCustomerQuota(
                CustomerQuotaFixtures.quota(
                        familyContext.family().getId(), familyContext.kid().getId(), kidUsed));
    }

    public record FamilyContext(
            Family family,
            Customer dad,
            Customer mom,
            Customer kid,
            FamilyMember dadMember,
            FamilyMember momMember,
            FamilyMember kidMember) {}
}
