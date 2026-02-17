package com.project.domain.usagerecord.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

import com.project.common.BuilderSupporter;
import com.project.common.TestFixtureBuilder;
import com.project.common.fixtures.CustomerFixtures;
import com.project.common.fixtures.CustomerQuotaFixtures;
import com.project.common.fixtures.FamilyMemberFixtures;
import com.project.domain.customer.entity.Customer;
import com.project.domain.customer.repository.CustomerQuotaQueryRepository;
import com.project.domain.customer.repository.impl.CustomerQuotaQueryRepositoryImpl;
import com.project.domain.family.entity.FamilyMember;
import com.project.domain.usagerecord.repository.CustomerQuotaQueryRepositoryTest.TestConfig;
import com.querydsl.jpa.impl.JPAQueryFactory;

@DataJpaTest
@Import({
    TestFixtureBuilder.class,
    BuilderSupporter.class,
    TestConfig.class,
    CustomerQuotaQueryRepositoryImpl.class
})
@ActiveProfiles("test")
class CustomerQuotaQueryRepositoryTest {

    @Autowired private CustomerQuotaQueryRepository repository;

    @Autowired private TestFixtureBuilder testFixtureBuilder;

    @Test
    void test_example() {
        // given
        Long familyId = 100L;

        Customer dad = testFixtureBuilder.buildCustomer(CustomerFixtures.dad());
        Customer mom = testFixtureBuilder.buildCustomer(CustomerFixtures.mom());
        Customer kid = testFixtureBuilder.buildCustomer(CustomerFixtures.kid());

        FamilyMember fm =
                testFixtureBuilder.buildFamilyMember(
                        FamilyMemberFixtures.member(familyId, dad.getId()));
        FamilyMember fm2 =
                testFixtureBuilder.buildFamilyMember(
                        FamilyMemberFixtures.member(familyId, mom.getId()));
        FamilyMember fm3 =
                testFixtureBuilder.buildFamilyMember(
                        FamilyMemberFixtures.member(familyId, kid.getId()));

        testFixtureBuilder.buildCustomerQuota(
                CustomerQuotaFixtures.quota(100L, dad.getId(), 400000));
    }

    @TestConfiguration
    @EnableJpaAuditing
    static class TestConfig {

        @PersistenceContext private EntityManager em;

        @Bean
        JPAQueryFactory jpaQueryFactory() {
            return new JPAQueryFactory(em);
        }
    }
}
