package com.devlink.controller;

import com.devlink.dto.request.UpdateProfileRequest;
import com.devlink.service.PostService;
import com.devlink.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final PostService postService;

    @GetMapping("/profile/{userId}")
    public ResponseEntity<?> getUserProfile(@PathVariable String userId) {
        return ResponseEntity.ok(userService.getUserProfile(userId));
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @AuthenticationPrincipal String userId,
            @RequestBody UpdateProfileRequest req) {
        userService.updateProfile(userId, req);
        return ResponseEntity.ok(Map.of("message", "Profile updated"));
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchUsers(@RequestParam("q") String query) {
        return ResponseEntity.ok(Map.of("users", userService.searchUsers(query)));
    }

 /*   @GetMapping("/stats/{userId}")
    public ResponseEntity<?> getUserStats(@PathVariable String userId) {
        return ResponseEntity.ok(postService.getProfileStats(userId));
    } */

    @GetMapping("/suggested")
    public ResponseEntity<?> getSuggestedUsers(
            @AuthenticationPrincipal String userId,
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(Map.of("users", userService.getSuggestedUsers(userId, limit)));
    }
}
