package com.petmate.server.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petmate.server.dto.ChatMessagePayload;
import com.petmate.server.dto.MessageResponse;
import com.petmate.server.service.ChatService;
import com.petmate.server.service.FirebaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketHandler.class);

    private final ObjectMapper objectMapper;
    private final ChatService chatService;
    private final FirebaseService firebaseService;
    
    // Maps userId to their WebSocketSession
    private final ConcurrentHashMap<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public ChatWebSocketHandler(ObjectMapper objectMapper, ChatService chatService, FirebaseService firebaseService) {
        this.objectMapper = objectMapper;
        this.chatService = chatService;
        this.firebaseService = firebaseService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // Extract userId from URI parameters, e.g. /ws/chat?userId=1
        String query = session.getUri().getQuery();
        if (query != null && query.contains("userId=")) {
            try {
                Long userId = Long.parseLong(query.split("userId=")[1]);
                sessions.put(userId, session);
                log.info("User {} connected to WebSocket", userId);
            } catch (Exception e) {
                log.error("Invalid userId in WebSocket URI");
                session.close();
            }
        } else {
            log.error("Missing userId in WebSocket URI");
            session.close();
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payloadStr = message.getPayload();
        log.info("Received WebSocket message: {}", payloadStr);
        
        try {
            ChatMessagePayload payload = objectMapper.readValue(payloadStr, ChatMessagePayload.class);
            
            if ("CHAT".equals(payload.getType())) {
                // Save to database
                MessageResponse savedMessage = chatService.saveMessage(payload);
                
                // Forward to recipient if online
                Long recipientId = payload.getRecipientId();
                if (recipientId != null) {
                    boolean isRecipientOnline = false;
                    if (sessions.containsKey(recipientId)) {
                        WebSocketSession recipientSession = sessions.get(recipientId);
                        if (recipientSession.isOpen()) {
                            String responseJson = objectMapper.writeValueAsString(savedMessage);
                            recipientSession.sendMessage(new TextMessage(responseJson));
                            isRecipientOnline = true;
                        }
                    }
                    
                    // Always send push notification for safety, or only when offline. 
                    // Let's send it always. The Android client can suppress it if the chat is open.
                    // Or we can send only if offline:
                    // if (!isRecipientOnline) { ... }
                    // Actually, sending it always is standard because the app might be in background but socket still open.
                    firebaseService.sendChatMessageNotification(savedMessage, recipientId);
                }
                
                // Also send back to sender as confirmation (optional but good for syncing)
                if (payload.getSenderId() != null && sessions.containsKey(payload.getSenderId())) {
                    WebSocketSession senderSession = sessions.get(payload.getSenderId());
                    if (senderSession.isOpen()) {
                        String responseJson = objectMapper.writeValueAsString(savedMessage);
                        senderSession.sendMessage(new TextMessage(responseJson));
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error processing WebSocket message", e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.values().removeIf(s -> s.getId().equals(session.getId()));
        log.info("WebSocket connection closed: {}", session.getId());
    }
}
