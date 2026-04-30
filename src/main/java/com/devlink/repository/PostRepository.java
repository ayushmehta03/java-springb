package com.devlink.repository;

import com.devlink.model.Post;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends MongoRepository<Post, String> {
    Optional<Post> findBySlugAndPublishedTrue(String slug);
    List<Post> findByPublishedTrue(Sort sort);
    List<Post> findByAuthorIdAndPublishedTrue(String authorId, Sort sort);
    List<Post> findByAuthorIdAndPublishedFalse(String authorId);
    List<Post> findByTagsRegexAndPublishedTrue(String tagRegex);

    // For trending: top by viewCount
    List<Post> findTop10ByPublishedTrueOrderByViewCountDesc();

    // For home feed: latest 3
    List<Post> findTop3ByPublishedTrueOrderByCreatedAtDesc();
}
