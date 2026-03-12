package com.project.global.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MaskingUtils {

    public static String maskCouponCode(String couponCode) {
        if (couponCode == null) {
            return null;
        }
        if (couponCode.length() <= 5) {
            return couponCode;
        }
        return couponCode.substring(0, 5) + "****";
    }
}
