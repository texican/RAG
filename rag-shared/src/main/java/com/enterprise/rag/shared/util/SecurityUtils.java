package com.enterprise.rag.shared.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

public class SecurityUtils {

    private static final PasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder(12);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public static String hashPassword(String plainPassword) {
        return PASSWORD_ENCODER.encode(plainPassword);
    }

    public static boolean verifyPassword(String plainPassword, String hashedPassword) {
        return PASSWORD_ENCODER.matches(plainPassword, hashedPassword);
    }

    public static String generateApiKey() {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        return "rag_" + Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    public static String generateVerificationToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static String generateSecureToken(int length) {
        byte[] randomBytes = new byte[length];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    public static boolean isValidPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        
        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;
        
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else if (!Character.isLetterOrDigit(c)) hasSpecial = true;
        }
        
        return hasUpper && hasLower && hasDigit && hasSpecial;
    }

    public static String sanitizeSlug(String input) {
        if (input == null) return "";
        
        return input.toLowerCase()
            .replaceAll("[^a-z0-9\\-]", "-")
            .replaceAll("-+", "-")
            .replaceAll("^-|-$", "");
    }
}