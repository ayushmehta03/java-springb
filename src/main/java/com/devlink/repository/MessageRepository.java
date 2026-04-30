package com.devlink.repository;

import com.devlink.model.Message;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MessageRepository extends MongoRepository<Message, String> {

    List<Message> findByRoomId(String roomId, Sort sort);

    Optional<Message> findTopByRoomIdOrderByCreatedAtDesc(String roomId);

    // Count unread messages in a room not sent by the given user
    @Query(value = "{ 'room_id': ?0, 'sender_id': { '$ne': ?1 }, 'seen_at': null }", count = true)
    long countUnreadByRoomIdAndNotSender(String roomId, String senderId);

    // Count unread across many rooms not sent by given user
    @Query(value = "{ 'room_id': { '$in': ?0 }, 'sender_id': { '$ne': ?1 }, 'seen_at': null }", count = true)
    long countUnreadAcrossRooms(List<String> roomIds, String senderId);
}
