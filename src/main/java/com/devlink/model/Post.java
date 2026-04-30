package com.devlink.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "posts")
public class Post {

    @Id
    private String id;

    @Field("author_id")
    private String authorId;

    @Field("title")
    private String title;

    @Field("slug")
    private String slug;

    @Field("content")
    private String content;

    @Field("image_url")
    private String imageUrl;

    @Field("tags")
    private List<String> tags;

    @Field("published")
    private boolean published;

    @Field("view_count")
    private long viewCount;

    @Field("created_at")
    private Instant createdAt;

    @Field("updated_at")
    private Instant updatedAt;
}
