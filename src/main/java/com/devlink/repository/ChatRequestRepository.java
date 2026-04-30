package com.devlink.repository;

import com.devlink.model.ChatRequest;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRequestRepository extends MongoRepository<ChatRequest, String> {

    long countBySenderIdAndReceiverIdAndStatus(String senderId, String receiverId, String status);

    List<ChatRequest> findByReceiverIdAndStatus(String receiverId, String status);

    long countByReceiverIdAndStatus(String receiverId, String status);

    @Query("{ '$or': [ { 'sender_id': ?0, 'receiver_id': ?1 }, { 'sender_id': ?1, 'receiver_id': ?0 } ], 'status': 'pending' }")
    Optional<ChatRequest> findPendingBetween(String userA, String userB);
}
