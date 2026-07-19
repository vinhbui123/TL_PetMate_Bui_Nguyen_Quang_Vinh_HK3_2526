package com.petmate.server.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petmate.server.service.SystemLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CustomAuthEntryPoint implements AuthenticationEntryPoint {

    private final SystemLogService systemLogService;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        String ip = getClientIp(request);

        systemLogService.warn(
                "INVALID_TOKEN",
                "UNAUTHENTICATED [IP: " + ip + "]",
                "Unauthorized 401: Missing or invalid token for [" + request.getMethod() + " " + request.getRequestURI() + "] - " + authException.getMessage()
        );

        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        new ObjectMapper().writeValue(response.getWriter(), Map.of(
                "error", "Unauthorized",
                "message", "Token không hợp lệ hoặc đã hết hạn."
        ));
    }

    private String getClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isEmpty()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
