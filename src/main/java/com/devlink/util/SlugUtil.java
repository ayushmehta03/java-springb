package com.devlink.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class SlugUtil {

    private static final SecureRandom random = new SecureRandom();

    /**
     * Converts a title to a URL-safe slug.
     */
    public String generateSlug(String title) {
        String slug = title.toLowerCase();
        slug = slug.replaceAll("[^a-z0-9 ]+", "");
        slug = slug.replaceAll("\\s+", "-");
        slug = slug.replaceAll("^-|-$", "");
        return slug;
    }

    /**
     * Appends 2 random bytes as hex to ensure uniqueness.
     */
    public String generateUniqueSlug(String title) {
        byte[] bytes = new byte[2];
        random.nextBytes(bytes);
        return generateSlug(title) + "-" + bytesToHex(bytes);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
