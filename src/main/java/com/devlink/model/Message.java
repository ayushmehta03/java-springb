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
@Document(collection = "messages")
public class Message {

    @Id
    private String id;

    @Field("room_id")
    private String roomId;

    @Field("sender_id")
    private String senderId;

    @Field("content")
    private String content;

    @Field("seen_at")
    private Instant seenAt;

    @Field("created_at")
    private Instant createdAt;
}
