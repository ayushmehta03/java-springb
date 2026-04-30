package com.devlink.service;

import com.devlink.dto.request.UpdateProfileRequest;
import com.devlink.dto.response.ResponseDto;
import com.devlink.model.Post;
import com.devlink.model.User;
import com.devlink.repository.PostRepository;
import com.devlink.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.*;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final MongoTemplate mongoTemplate;

    public Map<String, Object> getUserProfile(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "User not found"));

        List<Post> posts = postRepository.findByAuthorIdAndPublishedTrue(
                user.getId(), Sort.by(Sort.Direction.DESC, "createdAt"));

        ResponseDto.UserProfile profile = new ResponseDto.UserProfile(
                user.getUserName(), user.getBio(), user.getProfileImage(), user.getLastSeen());

        return Map.of("user", profile, "posts", posts);
    }

    public void updateProfile(String userId, UpdateProfileRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "User not found"));

        boolean changed = false;
        if (req.getUsername() != null)     { user.setUserName(req.getUsername()); changed = true; }
        if (req.getBio() != null)          { user.setBio(req.getBio()); changed = true; }
        if (req.getProfileImage() != null) { user.setProfileImage(req.getProfileImage()); changed = true; }

        if (!changed) throw new ResponseStatusException(BAD_REQUEST, "Nothing to update");

        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
    }

    public List<ResponseDto.SearchUser> searchUsers(String query) {
        if (query == null || query.trim().length() < 2) {
            throw new ResponseStatusException(BAD_REQUEST, "Query missing");
        }
        Query q = Query.query(Criteria.where("userName").regex(query.trim(), "i"));
        List<User> users = mongoTemplate.find(q, User.class);
        return users.stream()
                .map(u -> new ResponseDto.SearchUser(
                        u.getId(), u.getUserName(), u.getBio(), u.getProfileImage()))
                .collect(Collectors.toList());
    }

    public List<ResponseDto.SuggestedUser> getSuggestedUsers(String currentUserId, int limit) {
        Query q = Query.query(Criteria.where("id").ne(currentUserId))
                .limit(limit)
                .with(Sort.by(Sort.Direction.DESC, "createdAt"));
        List<User> users = mongoTemplate.find(q, User.class);
        return users.stream()
                .map(u -> new ResponseDto.SuggestedUser(
                        u.getId(), u.getUserId(), u.getUserName(), u.getProfileImage()))
                .collect(Collectors.toList());
    }
}
