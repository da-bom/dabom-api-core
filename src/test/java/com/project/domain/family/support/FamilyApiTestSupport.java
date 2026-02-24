package com.project.domain.family.support;

import java.util.concurrent.atomic.AtomicInteger;

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

    private static final AtomicInteger PHONE_SEQUENCE = new AtomicInteger(10_000_000);

    private final TestFixtureBuilder testFixtureBuilder;

    public FamilyApiTestSupport(TestFixtureBuilder testFixtureBuilder) {
        this.testFixtureBuilder = testFixtureBuilder;
    }

    public FamilyContext buildFamilyContext(String familyName) {
        Customer dad = testFixtureBuilder.buildCustomer(createCustomer(CustomerFixtures.DAD_NAME));
        Customer mom = testFixtureBuilder.buildCustomer(createCustomer(CustomerFixtures.MOM_NAME));
        Customer kid = testFixtureBuilder.buildCustomer(createCustomer(CustomerFixtures.KID_NAME));

        Family family = testFixtureBuilder.buildFamily(FamilyFixtures.family(dad.getId()));
        family.changeName(familyName);

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

    private Customer createCustomer(String name) {
        return new Customer(
                "010" + String.format("%08d", PHONE_SEQUENCE.getAndIncrement()),
                CustomerFixtures.DEFAULT_PASSWORD_HASH,
                name);
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
