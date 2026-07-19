package com.petmate.server.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotService {

    @Value("${groq.api.key}")
    private String apiKey;

    @Value("${groq.api.url}")
    private String apiUrl;

    @Value("${groq.model.id}")
    private String modelId;

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public String askChatbot(String userMessage) {
        try {
            Map<String, Object> requestBodyMap = Map.of(
                    "model", modelId,
                    "messages", List.of(
                            Map.of("role", "system", "content", "Bạn là một chuyên gia tư vấn thú y và chăm sóc vật nuôi. Hãy giải đáp các thắc mắc của người dùng về thú cưng một cách chính xác, thân thiện và hữu ích."),
                            Map.of("role", "user", "content", userMessage)
                    ),
                    "temperature", 0.7
            );

            String requestBody = objectMapper.writeValueAsString(requestBodyMap);

            HttpRequest request;
            request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                JsonNode rootNode = objectMapper.readTree(response.body());
                JsonNode choicesNode = rootNode.path("choices");
                if (choicesNode.isArray() && choicesNode.size() > 0) {
                    JsonNode messageNode = choicesNode.get(0).path("message");
                    return messageNode.path("content").asText();
                }
            } else {
                log.error("Groq API Error: HTTP {} - {}", response.statusCode(), response.body());
                return "Xin lỗi, tôi không thể xử lý yêu cầu lúc này. Vui lòng thử lại sau.";
            }
        } catch (Exception e) {
            log.error("Exception when calling Groq API", e);
            return "Xin lỗi, đã xảy ra lỗi hệ thống trong quá trình kết nối tới AI.";
        }
        return "Xin lỗi, tôi không thể trả lời câu hỏi này.";
    }
}
