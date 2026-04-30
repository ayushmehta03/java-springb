package com.devlink.dto.request;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String username;
    private String bio;
    private String profileImage;
}
