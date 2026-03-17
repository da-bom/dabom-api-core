package com.project.domain.customer.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.project.domain.customer.entity.Customer;
import com.project.domain.customer.model.CustomerMe;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Customer findByPhoneNumber(String phoneNumber);

    boolean existsByPhoneNumber(String phoneNumber);

    @Query(
            "SELECT new com.project.domain.customer.model.CustomerMe("
                    + "c.id, c.name, c.phoneNumber, fm.familyId, fm.role) "
                    + "FROM Customer c, FamilyMember fm "
                    + "WHERE c.id = fm.customerId AND c.id = :customerId")
    Optional<CustomerMe> findCustomerMeById(@Param("customerId") Long customerId);
}
