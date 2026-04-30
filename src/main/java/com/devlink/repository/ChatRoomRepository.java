package com.devlink.repository;

import com.devlink.model.ChatRoom;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends MongoRepository<ChatRoom, String> {

    List<ChatRoom> findByParticipantsContaining(String userId);

    long countByIdAndParticipantsContaining(String roomId, String userId);

    @Query("{ 'participants': { '$all': [?0, ?1] } }")
    Optional<ChatRoom> findByBothParticipants(String userA, String userB);
}
