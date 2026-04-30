package com.devlink.dto.response;

import com.devlink.model.Post;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

public class ResponseDto {

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PostAuthor {
        private String id;
        private String username;
        private String profileImage;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PostWithAuthor {
        private Post post;
        private PostAuthor author;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserProfile {
        private String name;
        private String bio;
        private String profileImage;
        private Instant lastSeen;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MeResponse {
        private String id;
        private String username;
        private String email;
        private String profileImage;
        private String role;
        private Instant createdAt;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SuggestedUser {
        private String id;
        private String userId;
        private String username;
        private String profileImage;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SearchUser {
        private String id;
        private String username;
        private String bio;
        private String profileImage;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ChatRoomItem {
        private String roomId;
        private String userId;
        private String lastMessage;
        private String lastSenderId;
        private Instant lastSeenAt;
        private Instant updatedAt;
        private long unread;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ChatCounts {
        private long requests;
        private long unreadMessages;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProfileStats {
        private long totalPosts;
        private long totalViews;
    }
}
