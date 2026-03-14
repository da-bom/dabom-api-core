package com.project.common.auth;

import org.mindrot.jbcrypt.BCrypt;

public final class PasswordHash {
    private PasswordHash() {}

    public static String hash(String rawPassword) {
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt(12));
    }

    public static boolean matches(String rawPassword, String hashed) {
        if (rawPassword == null || hashed == null) {
            return false;
        }
        return BCrypt.checkpw(rawPassword, hashed);
    }
}
