package com.devlink.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

public class ChatRequestDto {

    @Data
    public static class Send {
        @NotBlank
        private String receiverId;

        @NotBlank
        private String msg;
    }

    @Data
    public static class Respond {
        @NotBlank
        private String action; // "accept" | "reject"
    }
}
