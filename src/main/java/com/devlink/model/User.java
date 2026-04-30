package com.devlink.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User {

    @Id
    private String id;

    @Field("user_id")
    private String userId;

    @Field("username")
    private String userName;

    @Indexed(unique = true)
    @Field("email")
    private String email;

    @Field("password")
    private String password;

    @Field("bio")
    private String bio;

    @Field("profile_image")
    private String profileImage;

    @Field("role")
    private String role;

    @Field("is_verified")
    private boolean isVerified;

    @Field("otp_hash")
    private String otpHash;

    @Field("otp_expiry")
    private Instant otpExpiry;

    @Field("last_seen")
    private Instant lastSeen;

    @Field("created_at")
    private Instant createdAt;

    @Field("updated_at")
    private Instant updatedAt;
}
