package com.petmate.server.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petmate.server.service.SystemLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final SystemLogService systemLogService;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        String actor = request.getUserPrincipal() != null
                ? request.getUserPrincipal().getName()
                : "UNKNOWN";

        systemLogService.warn(
                "UNAUTHORIZED_ACCESS",
                actor,
                "Forbidden 403: Attempted to access [" + request.getMethod() + " " + request.getRequestURI() + "] without sufficient role."
        );

        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        new ObjectMapper().writeValue(response.getWriter(), Map.of(
                "error", "Forbidden",
                "message", "Báº¡n khÃ´ng cÃ³ quyá»n thá»±c hiá»‡n hÃ nh Ä‘á»™ng nÃ y."
        ));
    }
}
