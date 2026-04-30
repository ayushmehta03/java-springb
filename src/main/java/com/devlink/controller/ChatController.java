package com.devlink.controller;

import com.devlink.dto.request.ChatRequestDto;
import com.devlink.service.ChatService;
import com.devlink.util.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final JwtUtil jwtUtil;

    @PostMapping("/request")
    public ResponseEntity<?> sendChatRequest(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody ChatRequestDto.Send req) {
        chatService.sendChatRequest(userId, req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Chat request sent"));
    }

    @GetMapping("/requests")
    public ResponseEntity<?> getReceivedRequests(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(chatService.getReceivedRequests(userId));
    }

    @PostMapping("/requests/{id}/respond")
    public ResponseEntity<?> respondToRequest(
            @PathVariable String id,
            @AuthenticationPrincipal String userId,
            @RequestBody ChatRequestDto.Respond req) {
        return ResponseEntity.ok(chatService.respondToRequest(id, userId, req));
    }

    @GetMapping("/rooms")
    public ResponseEntity<?> getChatRooms(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(Map.of("rooms", chatService.getChatRooms(userId)));
    }

    @GetMapping("/history/{roomId}")
    public ResponseEntity<?> getChatHistory(
            @PathVariable String roomId,
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(chatService.getChatHistory(roomId, userId));
    }

    @PostMapping("/history/{roomId}/seen")
    public ResponseEntity<?> markSeen(
            @PathVariable String roomId,
            @AuthenticationPrincipal String userId) {
        chatService.markSeen(roomId, userId);
        return ResponseEntity.ok(Map.of("message", "Messages marked as seen"));
    }

    @GetMapping("/status/{userId}")
    public ResponseEntity<?> getChatRequestStatus(
            @PathVariable String userId,
            @AuthenticationPrincipal String currentUserId) {
        return ResponseEntity.ok(chatService.getChatRequestStatus(currentUserId, userId));
    }

    @GetMapping("/counts")
    public ResponseEntity<?> getChatCounts(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(chatService.getChatCounts(userId));
    }

    @GetMapping("/ws-token")
    public ResponseEntity<?> getWsToken(@AuthenticationPrincipal String userId) {
        String token = jwtUtil.generateWsToken(userId);
        return ResponseEntity.ok(Map.of("token", token));
    }
}
