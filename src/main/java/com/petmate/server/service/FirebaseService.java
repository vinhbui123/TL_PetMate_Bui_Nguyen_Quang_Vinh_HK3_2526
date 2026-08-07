package com.petmate.server.service;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.petmate.server.dto.MessageResponse;
import com.petmate.server.entity.DeviceToken;
import com.petmate.server.entity.User;
import com.petmate.server.repository.DeviceTokenRepository;
import com.petmate.server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FirebaseService {

    private final FirebaseMessaging firebaseMessaging;
    private final DeviceTokenRepository deviceTokenRepository;
    private final UserRepository userRepository;

    /**
     * Only delete token if Firebase explicitly says it's unregistered/invalid.
     * Other errors (network, server, etc.) should NOT cause token deletion.
     */
    private boolean shouldDeleteToken(FirebaseMessagingException ex) {
        if (ex == null || ex.getMessagingErrorCode() == null) return false;
        MessagingErrorCode code = ex.getMessagingErrorCode();
        return code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.INVALID_ARGUMENT;
    }

    public void sendChatMessageNotification(MessageResponse message, Long recipientId) {
        try {
            List<String> deviceTokens = deviceTokenRepository.findByUserId(recipientId).stream()
                    .map(DeviceToken::getToken)
                    .collect(Collectors.toList());

            if (deviceTokens.isEmpty()) {
                log.debug("No FCM tokens found for user {}", recipientId);
                return;
            }

            User sender = userRepository.findById(message.getSenderId()).orElse(null);
            String senderName = sender != null ? sender.getFullName() : "Someone";

            MulticastMessage firebaseMessage = MulticastMessage.builder()
                    .addAllTokens(deviceTokens)
                    .setNotification(Notification.builder()
                            .setTitle(senderName)
                            .setBody(message.getContent())
                            .build())
                    .putData("type", "message")
                    .putData("messageId", String.valueOf(message.getId()))
                    .putData("roomId", String.valueOf(message.getRoomId()))
                    .putData("senderId", String.valueOf(message.getSenderId()))
                    .putData("senderName", senderName)
                    .putData("content", message.getContent())
                    .putData("title", senderName)
                    .putData("body", message.getContent())
                    .build();

            BatchResponse response = firebaseMessaging.sendEachForMulticastAsync(firebaseMessage).get();
            log.info("[Chat] FCM result: success={}, failure={}", response.getSuccessCount(), response.getFailureCount());

            // Only clean up truly invalid tokens
            if (response.getFailureCount() > 0) {
                for (int i = 0; i < response.getResponses().size(); i++) {
                    if (!response.getResponses().get(i).isSuccessful()) {
                        FirebaseMessagingException ex = response.getResponses().get(i).getException();
                        String failedToken = deviceTokens.get(i);
                        log.warn("[Chat] Token failed: {} - Error: {}", failedToken,
                                ex != null ? ex.getMessagingErrorCode() + ": " + ex.getMessage() : "unknown");
                        if (shouldDeleteToken(ex)) {
                            log.info("[Chat] Deleting invalid token: {}", failedToken);
                            deviceTokenRepository.deleteById(failedToken);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to send Firebase message", e);
        }
    }

    public void sendNotification(Long recipientId, String title, String body, java.util.Map<String, String> data) {
        try {
            List<String> deviceTokens = deviceTokenRepository.findByUserId(recipientId).stream()
                    .map(DeviceToken::getToken)
                    .collect(Collectors.toList());

            if (deviceTokens.isEmpty()) {
                log.debug("No FCM tokens found for user {}", recipientId);
                return;
            }

            MulticastMessage.Builder messageBuilder = MulticastMessage.builder()
                    .addAllTokens(deviceTokens)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .putData("title", title)
                    .putData("body", body);

            if (data != null && !data.isEmpty()) {
                messageBuilder.putAllData(data);
            }

            BatchResponse response = firebaseMessaging.sendEachForMulticastAsync(messageBuilder.build()).get();
            log.info("[Notification] FCM result: success={}, failure={}", response.getSuccessCount(), response.getFailureCount());

            if (response.getFailureCount() > 0) {
                for (int i = 0; i < response.getResponses().size(); i++) {
                    if (!response.getResponses().get(i).isSuccessful()) {
                        FirebaseMessagingException ex = response.getResponses().get(i).getException();
                        String failedToken = deviceTokens.get(i);
                        log.warn("[Notification] Token failed: {} - Error: {}", failedToken,
                                ex != null ? ex.getMessagingErrorCode() + ": " + ex.getMessage() : "unknown");
                        if (shouldDeleteToken(ex)) {
                            log.info("[Notification] Deleting invalid token: {}", failedToken);
                            deviceTokenRepository.deleteById(failedToken);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to send Firebase notification", e);
        }
    }

    public void broadcastNotification(String title, String body) {
        try {
            List<String> allTokens = deviceTokenRepository.findAll().stream()
                    .map(DeviceToken::getToken)
                    .collect(Collectors.toList());

            log.info("[Broadcast] Found {} FCM tokens in database.", allTokens.size());

            if (allTokens.isEmpty()) {
                log.info("No FCM tokens found for broadcast.");
                return;
            }

            // Firebase limits MulticastMessage to 500 tokens per batch
            int batchSize = 500;
            for (int i = 0; i < allTokens.size(); i += batchSize) {
                List<String> batchTokens = allTokens.subList(i, Math.min(i + batchSize, allTokens.size()));
                
                log.info("[Broadcast] Sending batch of {} tokens...", batchTokens.size());

                MulticastMessage message = MulticastMessage.builder()
                        .addAllTokens(batchTokens)
                        .setNotification(Notification.builder()
                                .setTitle(title)
                                .setBody(body)
                                .build())
                        .putData("type", "broadcast")
                        .putData("title", title)
                        .putData("body", body)
                        .build();
                        
                BatchResponse response = firebaseMessaging.sendEachForMulticastAsync(message).get();
                
                log.info("[Broadcast] Batch result: success={}, failure={}", response.getSuccessCount(), response.getFailureCount());

                // ONLY delete tokens with UNREGISTERED error, NOT all failures
                if (response.getFailureCount() > 0) {
                    for (int j = 0; j < response.getResponses().size(); j++) {
                        if (!response.getResponses().get(j).isSuccessful()) {
                            FirebaseMessagingException ex = response.getResponses().get(j).getException();
                            String failedToken = batchTokens.get(j);
                            log.warn("[Broadcast] Token failed: {} - Error: {}", failedToken,
                                    ex != null ? ex.getMessagingErrorCode() + ": " + ex.getMessage() : "unknown");
                            if (shouldDeleteToken(ex)) {
                                log.info("[Broadcast] Deleting invalid token: {}", failedToken);
                                deviceTokenRepository.deleteById(failedToken);
                            }
                        }
                    }
                }
            }
            log.info("Broadcast sent to {} devices.", allTokens.size());
        } catch (Exception e) {
            log.error("Failed to send broadcast notification: {}", e.getMessage(), e);
        }
    }
}
