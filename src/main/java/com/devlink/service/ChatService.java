package com.devlink.service;

import com.devlink.dto.request.ChatRequestDto;
import com.devlink.dto.response.ResponseDto;
import com.devlink.model.ChatRequest;
import com.devlink.model.ChatRoom;
import com.devlink.model.Message;
import com.devlink.repository.ChatRequestRepository;
import com.devlink.repository.ChatRoomRepository;
import com.devlink.repository.MessageRepository;
import com.devlink.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.*;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRequestRepository chatRequestRepo;
    private final ChatRoomRepository chatRoomRepo;
    private final MessageRepository messageRepo;
    private final UserRepository userRepo;
    private final MongoTemplate mongoTemplate;

    public void sendChatRequest(String senderId, ChatRequestDto.Send req) {
        String msg = req.getMsg() != null ? req.getMsg().trim() : "";
        if (msg.isEmpty()) throw new ResponseStatusException(BAD_REQUEST, "Message cannot be empty");
        if (senderId.equals(req.getReceiverId()))
            throw new ResponseStatusException(BAD_REQUEST, "Cannot send message request to self");

        long existing = chatRequestRepo.countBySenderIdAndReceiverIdAndStatus(
                senderId, req.getReceiverId(), "pending");
        if (existing > 0) throw new ResponseStatusException(CONFLICT, "Chat request already sent");

        ChatRequest request = new ChatRequest();
        request.setSenderId(senderId);
        request.setReceiverId(req.getReceiverId());
        request.setMsg(msg);
        request.setStatus("pending");
        request.setCreatedAt(Instant.now());
        chatRequestRepo.save(request);
    }

    public List<ChatRequest> getReceivedRequests(String userId) {
        return chatRequestRepo.findByReceiverIdAndStatus(userId, "pending");
    }

    public Map<String, Object> respondToRequest(String requestId, String userId, ChatRequestDto.Respond req) {
        if (!"accept".equals(req.getAction()) && !"reject".equals(req.getAction())) {
            throw new ResponseStatusException(BAD_REQUEST, "Invalid action");
        }

        ChatRequest chatReq = chatRequestRepo.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Chat request not found"));

        if (!chatReq.getReceiverId().equals(userId)) {
            throw new ResponseStatusException(FORBIDDEN, "Not allowed");
        }

        String status = "accept".equals(req.getAction()) ? "accepted" : "rejected";
        chatReq.setStatus(status);
        chatRequestRepo.save(chatReq);

        if ("accepted".equals(status)) {
            Optional<ChatRoom> existing = chatRoomRepo.findByBothParticipants(
                    chatReq.getSenderId(), chatReq.getReceiverId());
            if (existing.isPresent()) {
                return Map.of("status", "accepted", "room_id", existing.get().getId());
            }

            ChatRoom room = new ChatRoom();
            room.setParticipants(List.of(chatReq.getSenderId(), chatReq.getReceiverId()));
            room.setCreatedAt(Instant.now());
            chatRoomRepo.save(room);
            return Map.of("status", "accepted", "room_id", room.getId());
        }

        return Map.of("status", "rejected");
    }

    public List<Message> getChatHistory(String roomId, String userId) {
        long count = chatRoomRepo.countByIdAndParticipantsContaining(roomId, userId);
        if (count == 0) throw new ResponseStatusException(FORBIDDEN, "Not allowed");

        return messageRepo.findByRoomId(roomId, Sort.by(Sort.Direction.ASC, "createdAt"));
    }

    public void markSeen(String roomId, String userId) {
        Query query = Query.query(
                Criteria.where("roomId").is(roomId)
                        .and("senderId").ne(userId)
                        .and("seenAt").isNull()
        );
        mongoTemplate.updateMulti(query,
                Update.update("seenAt", Instant.now()), Message.class);
    }

    public Map<String, Object> getChatRequestStatus(String currentUserId, String otherUserId) {
        Optional<ChatRoom> room = chatRoomRepo.findByBothParticipants(currentUserId, otherUserId);
        if (room.isPresent()) {
            return Map.of("status", "accepted", "room_id", room.get().getId());
        }

        Optional<ChatRequest> req = chatRequestRepo.findPendingBetween(currentUserId, otherUserId);
        if (req.isPresent()) {
            String type = req.get().getSenderId().equals(currentUserId) ? "sent" : "received";
            return Map.of("status", "pending", "type", type);
        }

        return Map.of("status", "none");
    }

    public ResponseDto.ChatCounts getChatCounts(String userId) {
        long pending = chatRequestRepo.countByReceiverIdAndStatus(userId, "pending");

        List<ChatRoom> rooms = chatRoomRepo.findByParticipantsContaining(userId);
        List<String> roomIds = rooms.stream().map(ChatRoom::getId).collect(Collectors.toList());

        long unread = roomIds.isEmpty() ? 0
                : messageRepo.countUnreadAcrossRooms(roomIds, userId);

        return new ResponseDto.ChatCounts(pending, unread);
    }

    public List<ResponseDto.ChatRoomItem> getChatRooms(String userId) {
        List<ChatRoom> rooms = chatRoomRepo.findByParticipantsContaining(userId);

        return rooms.stream().map(room -> {
            String otherId = room.getParticipants().stream()
                    .filter(p -> !p.equals(userId))
                    .findFirst().orElse(null);

            Optional<Message> lastMsgOpt = messageRepo.findTopByRoomIdOrderByCreatedAtDesc(room.getId());

            if (lastMsgOpt.isEmpty()) {
                return new ResponseDto.ChatRoomItem(
                        room.getId(), otherId, null, null, null, null, 0);
            }

            Message lastMsg = lastMsgOpt.get();
            long unread = messageRepo.countUnreadByRoomIdAndNotSender(room.getId(), userId);

            return new ResponseDto.ChatRoomItem(
                    room.getId(),
                    otherId,
                    lastMsg.getContent(),
                    lastMsg.getSenderId(),
                    lastMsg.getSeenAt(),
                    lastMsg.getCreatedAt(),
                    unread
            );
        }).collect(Collectors.toList());
    }
}
