package com.devlink.service;

import com.devlink.dto.request.PostRequest;
import com.devlink.dto.response.ResponseDto;
import com.devlink.model.Post;
import com.devlink.model.User;
import com.devlink.repository.PostRepository;
import com.devlink.repository.UserRepository;
import com.devlink.util.SlugUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.*;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final SlugUtil slugUtil;
    private final MongoTemplate mongoTemplate;

    public List<ResponseDto.PostWithAuthor> getHomeFeed() {
        List<Post> posts = postRepository.findTop3ByPublishedTrueOrderByCreatedAtDesc();
        return enrichWithAuthor(posts);
    }

    public List<ResponseDto.PostWithAuthor> getTrendingPosts() {
        List<Post> posts = postRepository.findTop10ByPublishedTrueOrderByViewCountDesc();
        return enrichWithAuthor(posts);
    }

    public List<ResponseDto.PostWithAuthor> getAllPosts() {
        List<Post> posts = postRepository.findByPublishedTrue(Sort.by(Sort.Direction.DESC, "createdAt"));
        return enrichWithAuthor(posts);
    }

    public ResponseDto.PostWithAuthor getPostBySlug(String slug) {
        Query query = Query.query(Criteria.where("slug").is(slug).and("published").is(true));
        Update update = new Update().inc("viewCount", 1);
        Post post = mongoTemplate.findAndModify(query, update, Post.class);

        if (post == null) throw new ResponseStatusException(NOT_FOUND, "Post not found");

        User user = userRepository.findById(post.getAuthorId())
                .orElseThrow(() -> new ResponseStatusException(INTERNAL_SERVER_ERROR, "Author not found"));

        return toPostWithAuthor(post, user);
    }

    public ResponseDto.PostWithAuthor createPost(String userId, PostRequest.Create req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "User not found"));

        Post post = new Post();
        post.setAuthorId(userId);
        post.setTitle(req.getTitle());
        post.setContent(req.getContent());
        post.setImageUrl(req.getImageUrl());
        post.setTags(req.getTags());
        post.setPublished(req.isPublished());
        post.setSlug(slugUtil.generateUniqueSlug(req.getTitle()));
        post.setViewCount(0);
        post.setCreatedAt(Instant.now());
        post.setUpdatedAt(Instant.now());

        postRepository.save(post);
        return toPostWithAuthor(post, user);
    }

    public void updatePost(String postId, String userId, PostRequest.Update req) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Post not found"));

        if (!post.getAuthorId().equals(userId)) {
            throw new ResponseStatusException(FORBIDDEN, "Not allowed");
        }

        boolean changed = false;
        if (req.getTitle() != null) {
            post.setTitle(req.getTitle());
            post.setSlug(slugUtil.generateUniqueSlug(req.getTitle()));
            changed = true;
        }
        if (req.getContent() != null)  { post.setContent(req.getContent()); changed = true; }
        if (req.getImageUrl() != null) { post.setImageUrl(req.getImageUrl()); changed = true; }
        if (req.getTags() != null)     { post.setTags(req.getTags()); changed = true; }
        if (req.getPublished() != null){ post.setPublished(req.getPublished()); changed = true; }

        if (!changed) throw new ResponseStatusException(BAD_REQUEST, "Nothing to update");

        post.setUpdatedAt(Instant.now());
        postRepository.save(post);
    }

    public void deletePost(String postId, String userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Post not found"));

        if (!post.getAuthorId().equals(userId)) {
            throw new ResponseStatusException(FORBIDDEN, "Not allowed");
        }
        postRepository.deleteById(postId);
    }

    public List<Post> getMyPosts(String userId) {
        return postRepository.findByAuthorIdAndPublishedTrue(
                userId, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    public List<Post> getArchivePosts(String userId) {
        return postRepository.findByAuthorIdAndPublishedFalse(userId);
    }

    public List<ResponseDto.PostWithAuthor> searchPosts(String query) {
        if (query == null || query.trim().length() < 2) {
            throw new ResponseStatusException(BAD_REQUEST, "Search query missing");
        }
        Query q = Query.query(
                Criteria.where("tags").regex(query.trim(), "i")
                        .and("published").is(true)
        );
        List<Post> posts = mongoTemplate.find(q, Post.class);
        return enrichWithAuthor(posts);
    }

   /*   public Map<String, Object> getProfileStats(String userId) {
        Aggregation agg = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("authorId").is(userId).and("published").is(true)),
                Aggregation.group()
                        .count().as("totalPosts")
                        .sum("viewCount").as("totalViews")
        );
        AggregationResults<Map> results = mongoTemplate.aggregate(agg, "posts", Map.class);
        List<Map> mappedResults = results.getMappedResults();
        if (mappedResults.isEmpty()) {
            return Map.of("total_posts", 0, "total_views", 0);
        }
        Map<?, ?> r = mappedResults.get(0);
        return Map.of(
                "total_posts", r.getOrDefault("totalPosts", 0),
                "total_views", r.getOrDefault("totalViews", 0)
        );
    }
*/
    private List<ResponseDto.PostWithAuthor> enrichWithAuthor(List<Post> posts) {
        return posts.stream()
                .map(post -> {
                    User user = userRepository.findById(post.getAuthorId()).orElse(null);
                    if (user == null) return null;
                    return toPostWithAuthor(post, user);
                })
                .filter(p -> p != null)
                .collect(Collectors.toList());
    }

    private ResponseDto.PostWithAuthor toPostWithAuthor(Post post, User user) {
        ResponseDto.PostAuthor author = new ResponseDto.PostAuthor(
                user.getId(), user.getUserName(), user.getProfileImage());
        return new ResponseDto.PostWithAuthor(post, author);
    }
}
