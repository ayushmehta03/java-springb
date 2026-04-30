package com.devlink.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

public class PostRequest {

    @Data
    public static class Create {
        @NotBlank
        private String title;

        @NotBlank
        private String content;

        private String imageUrl;
        private List<String> tags;
        private boolean published;
    }

    @Data
    public static class Update {
        private String title;
        private String content;
        private String imageUrl;
        private List<String> tags;
        private Boolean published;
    }
}
