package com.project.global.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PhoneNumberUtils {

    public static String mask(String phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }
        String digits = phoneNumber.replaceAll("[^0-9]", "");
        if (digits.length() < 7) {
            return phoneNumber;
        }
        return digits.substring(0, 3) + "-****-" + digits.substring(digits.length() - 4);
    }
}
