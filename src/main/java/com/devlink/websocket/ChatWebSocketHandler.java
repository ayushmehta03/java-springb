package com.devlink.websocket;

import com.devlink.model.Message;
import com.devlink.repository.ChatRoomRepository;
import com.devlink.repository.MessageRepository;
import com.devlink.repository.UserRepository;
import com.devlink.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final JwtUtil jwtUtil;
    private final ChatRoomRepository chatRoomRepo;
    private final MessageRepository messageRepo;
    private final UserRepository userRepo;
    private final ObjectMapper objectMapper;

    private final ConcurrentHashMap<String, ConcurrentHashMap<WebSocketSession, String>> roomClients
            = new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String token = getQueryParam(session, "token");
        if (token == null) { session.close(CloseStatus.NOT_ACCEPTABLE); return; }

        Claims claims;
        try {
            claims = jwtUtil.parseToken(token);
        } catch (Exception e) {
            session.close(CloseStatus.NOT_ACCEPTABLE);
            return;
        }

        if (!jwtUtil.isWsToken(claims)) { session.close(CloseStatus.NOT_ACCEPTABLE); return; }

        String userIdHex = claims.get("user_id", String.class);
        String roomId = extractRoomId(session);

        long count = chatRoomRepo.countByIdAndParticipantsContaining(roomId, userIdHex);
        if (count == 0) { session.close(CloseStatus.NOT_ACCEPTABLE); return; }

        roomClients.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>()).put(session, userIdHex);
        session.getAttributes().put("userId", userIdHex);
        session.getAttributes().put("roomId", roomId);

        broadcast(roomId, Map.of("type", "user_online", "user_id", userIdHex), session);

        roomClients.get(roomId).forEach((s, uid) -> {
            if (!uid.equals(userIdHex)) {
                send(session, Map.of("type", "user_online", "user_id", uid));
            }
        });

        ScheduledFuture<?> pingTask = scheduler.scheduleAtFixedRate(() -> {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new PingMessage());
                }
            } catch (IOException e) {
                log.debug("Ping failed for session {}", session.getId());
            }
        }, 30, 30, TimeUnit.SECONDS);

        session.getAttributes().put("pingTask", pingTask);
        log.info("User {} connected to room {}", userIdHex, roomId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String userId = (String) session.getAttributes().get("userId");
        String roomId = (String) session.getAttributes().get("roomId");
        if (userId == null || roomId == null) return;

        Map<?, ?> payload;
        try {
            payload = objectMapper.readValue(message.getPayload(), Map.class);
        } catch (Exception e) {
            log.warn("Invalid JSON from {}: {}", userId, e.getMessage());
            return;
        }

        String type = (String) payload.get("type");
        if (type == null) return;

        switch (type) {
            case "typing" -> {
                boolean isTyping = Boolean.TRUE.equals(payload.get("is_typing"));
                broadcast(roomId, Map.of(
                        "type", "typing",
                        "user_id", userId,
                        "is_typing", isTyping
                ), null);
            }
            case "message" -> {
                String content = (String) payload.get("content");
                if (content == null || content.isBlank()) return;

                Message msg = new Message();
                msg.setRoomId(roomId);
                msg.setSenderId(userId);
                msg.setContent(content);
                msg.setCreatedAt(Instant.now());
                messageRepo.save(msg);

                broadcast(roomId, Map.of(
                        "type", "message",
                        "id", msg.getId(),
                        "room_id", roomId,
                        "sender_id", userId,
                        "content", msg.getContent(),
                        "created_at", msg.getCreatedAt().toString()
                ), null);
            }
            default -> log.debug("Unknown message type: {}", type);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String userId = (String) session.getAttributes().get("userId");
        String roomId = (String) session.getAttributes().get("roomId");

        ScheduledFuture<?> pingTask = (ScheduledFuture<?>) session.getAttributes().get("pingTask");
        if (pingTask != null) pingTask.cancel(true);

        if (roomId != null && roomClients.containsKey(roomId)) {
            roomClients.get(roomId).remove(session);
        }

        if (userId != null) {
            Instant now = Instant.now();
            userRepo.findById(userId).ifPresent(user -> {
                user.setLastSeen(now);
                userRepo.save(user);
            });

            if (roomId != null) {
                broadcast(roomId, Map.of(
                        "type", "user_offline",
                        "user_id", userId,
                        "last_seen", now.toString()
                ), null);
            }
        }
        log.info("User {} disconnected from room {}", userId, roomId);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("WebSocket transport error for session {}: {}", session.getId(), exception.getMessage());
    }

    private void broadcast(String roomId, Map<String, Object> payload, WebSocketSession exclude) {
        ConcurrentHashMap<WebSocketSession, String> clients = roomClients.get(roomId);
        if (clients == null) return;

        clients.forEach((sess, uid) -> {
            if (exclude != null && sess == exclude) return;
            send(sess, payload);
        });
    }

    private void send(WebSocketSession session, Map<String, ?> payload) {
        if (!session.isOpen()) return;
        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
        } catch (IOException e) {
            log.debug("Failed to send to session {}: {}", session.getId(), e.getMessage());
        }
    }

    private String extractRoomId(WebSocketSession session) {
        String path = session.getUri().getPath();
        String[] parts = path.split("/");
        return parts[parts.length - 1];
    }

    private String getQueryParam(WebSocketSession session, String param) {
        String query = session.getUri().getQuery();
        if (query == null) return null;
        for (String part : query.split("&")) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2 && kv[0].equals(param)) return kv[1];
        }
        return null;
    }
}
