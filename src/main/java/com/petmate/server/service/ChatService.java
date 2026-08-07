package com.petmate.server.service;

import com.petmate.server.dto.*;
import com.petmate.server.entity.ChatMessage;
import com.petmate.server.entity.ChatRoom;
import com.petmate.server.entity.Pet;
import com.petmate.server.entity.User;
import com.petmate.server.enums.MessageStatus;
import com.petmate.server.repository.ChatMessageRepository;
import com.petmate.server.repository.ChatRoomRepository;
import com.petmate.server.repository.PetRepository;
import com.petmate.server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final PetRepository petRepository;
    private final ProfanityFilterService profanityFilterService;

    @Transactional
    public ChatRoomResponse getOrCreateRoom(Long buyerId, Long sellerId, Long petId) {
        Optional<ChatRoom> existingRoom = chatRoomRepository.findExistingRoom(buyerId, sellerId, petId);
        
        ChatRoom room;
        if (existingRoom.isPresent()) {
            room = existingRoom.get();
        } else {
            User buyer = userRepository.findById(buyerId).orElseThrow(() -> new RuntimeException("Buyer not found"));
            User seller = userRepository.findById(sellerId).orElseThrow(() -> new RuntimeException("KhÃ´ng tÃ¬m tháº¥y ngÆ°á»i bÃ¡n"));
            Pet pet = petRepository.findById(petId).orElseThrow(() -> new RuntimeException("KhÃ´ng tÃ¬m tháº¥y thÃº cÆ°ng"));

            room = ChatRoom.builder()
                    .buyer(buyer)
                    .seller(seller)
                    .pet(pet)
                    .build();
            room = chatRoomRepository.save(room);
        }
        
        return mapToChatRoomResponse(room, buyerId); // We assume buyerId is the requester
    }

    public List<ChatRoomResponse> getUserRooms(Long userId) {
        List<ChatRoom> rooms = chatRoomRepository.findByUserIdOrderByUpdatedAtDesc(userId);
        return rooms.stream()
                .map(room -> mapToChatRoomResponse(room, userId))
                .collect(Collectors.toList());
    }

    public List<MessageResponse> getRoomMessages(Long roomId) {
        return chatMessageRepository.findByRoomIdOrderByCreatedAtAsc(roomId).stream()
                .map(this::mapToMessageResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void markRoomMessagesAsRead(Long roomId, Long currentUserId) {
        List<ChatMessage> messages = chatMessageRepository.findByRoomIdOrderByCreatedAtAsc(roomId);
        boolean updated = false;
        for (ChatMessage msg : messages) {
            if (!msg.getSender().getId().equals(currentUserId) && msg.getStatus() != MessageStatus.READ) {
                msg.setStatus(MessageStatus.READ);
                updated = true;
            }
        }
        if (updated) {
            chatMessageRepository.saveAll(messages);
        }
    }

    public int getTotalUnreadCount(Long userId) {
        return chatMessageRepository.countTotalUnreadForUser(userId);
    }

    @Transactional
    public MessageResponse saveMessage(ChatMessagePayload payload) {
        ChatRoom room = chatRoomRepository.findById(payload.getRoomId())
                .orElseThrow(() -> new RuntimeException("Room not found"));
        User sender = userRepository.findById(payload.getSenderId())
                .orElseThrow(() -> new RuntimeException("Sender not found"));

        ChatMessage message = ChatMessage.builder()
                .room(room)
                .sender(sender)
                .content(profanityFilterService.filter(payload.getContent()))
                .build();
        
        message = chatMessageRepository.save(message);
        
        // Update room timestamp
        room.setUpdatedAt(LocalDateTime.now());
        chatRoomRepository.save(room);
        
        return mapToMessageResponse(message);
    }

    private ChatRoomResponse mapToChatRoomResponse(ChatRoom room, Long currentUserId) {
        User otherUser = room.getBuyer().getId().equals(currentUserId) ? room.getSeller() : room.getBuyer();
        
        ChatMessage lastMsg = chatMessageRepository.findTopByRoomIdOrderByCreatedAtDesc(room.getId());
        MessageResponse lastMsgResponse = lastMsg != null ? mapToMessageResponse(lastMsg) : null;
        
        int unreadCount = chatMessageRepository.countByRoomIdAndSenderIdNotAndStatusNot(room.getId(), currentUserId, MessageStatus.READ);
        
        return ChatRoomResponse.builder()
                .id(room.getId())
                .otherUser(otherUser)
                .pet(room.getPet())
                .lastMessage(lastMsgResponse)
                .unreadCount(unreadCount)
                .updatedAt(room.getUpdatedAt())
                .build();
    }

    private MessageResponse mapToMessageResponse(ChatMessage message) {
        return MessageResponse.builder()
                .id(message.getId())
                .roomId(message.getRoom().getId())
                .senderId(message.getSender().getId())
                .content(message.getContent())
                .createdAt(message.getCreatedAt())
                .status(message.getStatus())
                .build();
    }
}

