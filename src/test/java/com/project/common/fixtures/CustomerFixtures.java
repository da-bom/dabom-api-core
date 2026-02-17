package com.project.common.fixtures;

import com.project.domain.customer.entity.Customer;

public class CustomerFixtures {

    public static final String DAD_PHONE = "01012345678";
    public static final String MOM_PHONE = "01087654321";
    public static final String KID_PHONE = "01011112222";

    public static final String DEFAULT_PASSWORD_HASH = "pwHash";
    public static final String DAD_NAME = "아빠";
    public static final String MOM_NAME = "엄마";
    public static final String KID_NAME = "아이";

    public static Customer dad() {
        return new Customer(DAD_PHONE, DEFAULT_PASSWORD_HASH, DAD_NAME);
    }

    public static Customer mom() {
        return new Customer(MOM_PHONE, DEFAULT_PASSWORD_HASH, MOM_NAME);
    }

    public static Customer kid() {
        return new Customer(KID_PHONE, DEFAULT_PASSWORD_HASH, KID_NAME);
    }
}
