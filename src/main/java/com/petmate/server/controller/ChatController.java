package com.petmate.server.controller;

import com.petmate.server.dto.ChatRoomResponse;
import com.petmate.server.dto.MessageResponse;
import com.petmate.server.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/rooms")
    public ResponseEntity<List<ChatRoomResponse>> getUserRooms(@RequestParam("userId") Long userId) {
        // Ideally userId should come from JWT @AuthenticationPrincipal
        return ResponseEntity.ok(chatService.getUserRooms(userId));
    }

    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<List<MessageResponse>> getRoomMessages(@PathVariable Long roomId) {
        return ResponseEntity.ok(chatService.getRoomMessages(roomId));
    }

    @PostMapping("/rooms/start")
    public ResponseEntity<ChatRoomResponse> getOrCreateRoom(@RequestBody Map<String, Long> payload) {
        Long buyerId = payload.get("buyerId");
        Long sellerId = payload.get("sellerId");
        Long petId = payload.get("petId");
        
        if (buyerId == null || sellerId == null || petId == null) {
            return ResponseEntity.badRequest().build();
        }
        
        return ResponseEntity.ok(chatService.getOrCreateRoom(buyerId, sellerId, petId));
    }

    @PutMapping("/rooms/{roomId}/read")
    public ResponseEntity<Void> markRoomAsRead(@PathVariable Long roomId, @RequestParam("userId") Long userId) {
        chatService.markRoomMessagesAsRead(roomId, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Integer> getTotalUnreadCount(@RequestParam("userId") Long userId) {
        return ResponseEntity.ok(chatService.getTotalUnreadCount(userId));
    }
}
