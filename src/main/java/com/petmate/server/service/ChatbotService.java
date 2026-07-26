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
                            Map.of("role", "system", "content", "Báº¡n lÃ  má»™t chuyÃªn gia tÆ° váº¥n thÃº y vÃ  chÄƒm sÃ³c váº­t nuÃ´i. HÃ£y giáº£i Ä‘Ã¡p cÃ¡c tháº¯c máº¯c cá»§a ngÆ°á»i dÃ¹ng vá» thÃº cÆ°ng má»™t cÃ¡ch chÃ­nh xÃ¡c, thÃ¢n thiá»‡n vÃ  há»¯u Ã­ch."),
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
                return "Xin lá»—i, tÃ´i khÃ´ng thá»ƒ xá»­ lÃ½ yÃªu cáº§u lÃºc nÃ y. Vui lÃ²ng thá»­ láº¡i sau.";
            }
        } catch (Exception e) {
            log.error("Exception when calling Groq API", e);
            return "Xin lá»—i, Ä‘Ã£ xáº£y ra lá»—i há»‡ thá»‘ng trong quÃ¡ trÃ¬nh káº¿t ná»‘i tá»›i AI.";
        }
        return "Xin lá»—i, tÃ´i khÃ´ng thá»ƒ tráº£ lá»i cÃ¢u há»i nÃ y.";
    }
}
