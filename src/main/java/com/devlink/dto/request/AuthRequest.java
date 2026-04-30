package com.devlink.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

public class AuthRequest {

    @Data
    public static class Register {
        @NotBlank
        private String userName;

        @NotBlank
        @Email
        private String email;

        @NotBlank
        @Size(min = 6)
        private String password;
    }

    @Data
    public static class Login {
        @NotBlank
        @Email
        private String email;

        @NotBlank
        private String password;
    }

    @Data
    public static class VerifyOtp {
        @NotBlank
        @Email
        private String email;

        @NotBlank
        private String otp;
    }

    @Data
    public static class ResendOtp {
        @NotBlank
        @Email
        private String email;
    }
}
