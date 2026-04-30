package com.devlink.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "chat_requests")
public class ChatRequest {

    @Id
    private String id;

    @Field("sender_id")
    private String senderId;

    @Field("receiver_id")
    private String receiverId;

    @Field("msg")
    private String msg;

    // "pending" | "accepted" | "rejected"
    @Field("status")
    private String status;

    @Field("created_at")
    private Instant createdAt;
}
