package com.devlink.controller;

import com.devlink.dto.request.PostRequest;
import com.devlink.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @GetMapping("/home")
    public ResponseEntity<?> getHomeFeed() {
        return ResponseEntity.ok(Map.of("posts", postService.getHomeFeed()));
    }

    @GetMapping("/trending")
    public ResponseEntity<?> getTrendingPosts() {
        return ResponseEntity.ok(Map.of("posts", postService.getTrendingPosts()));
    }

    @GetMapping
    public ResponseEntity<?> getAllPosts() {
        return ResponseEntity.ok(postService.getAllPosts());
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<?> getPostBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(postService.getPostBySlug(slug));
    }

    @PostMapping
    public ResponseEntity<?> createPost(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody PostRequest.Create req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(postService.createPost(userId, req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updatePost(
            @PathVariable String id,
            @AuthenticationPrincipal String userId,
            @RequestBody PostRequest.Update req) {
        postService.updatePost(id, userId, req);
        return ResponseEntity.ok(Map.of("message", "Post updated"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePost(
            @PathVariable String id,
            @AuthenticationPrincipal String userId) {
        postService.deletePost(id, userId);
        return ResponseEntity.ok(Map.of("message", "Post deleted"));
    }

    @GetMapping("/my")
    public ResponseEntity<?> getMyPosts(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(Map.of("posts", postService.getMyPosts(userId)));
    }

    @GetMapping("/archive")
    public ResponseEntity<?> getArchivePosts(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(postService.getArchivePosts(userId));
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchPosts(@RequestParam("t") String query) {
        return ResponseEntity.ok(Map.of("posts", postService.searchPosts(query)));
    }
}
