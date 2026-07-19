package com.petmate.server.controller;

import com.petmate.server.dto.ChatbotRequest;
import com.petmate.server.dto.ChatbotResponse;
import com.petmate.server.service.ChatbotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;

    @PostMapping("/ask")
    public ResponseEntity<ChatbotResponse> askChatbot(@RequestBody ChatbotRequest request) {
        if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        String reply = chatbotService.askChatbot(request.getMessage());
        return ResponseEntity.ok(new ChatbotResponse(reply));
    }
}
