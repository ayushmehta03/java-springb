package com.devlink.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class OtpUtil {

    private static final SecureRandom random = new SecureRandom();

    /**
     * Generates a 6-digit zero-padded OTP string.
     */
    public String generateOtp() {
        int otp = random.nextInt(1_000_000);
        return String.format("%06d", otp);
    }
}
