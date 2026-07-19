package com.petmate.server.config;

import com.petmate.server.service.SystemLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.Map;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final SystemLogService systemLogService;

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleAllExceptions(Exception ex, WebRequest request) {
        String requestDesc = request.getDescription(false);
        log.error("[GlobalExceptionHandler] Unhandled error on {}: {}", requestDesc, ex.getMessage(), ex);

        systemLogService.error(
                "UNHANDLED_EXCEPTION",
                "SYSTEM",
                "Error at [" + requestDesc + "]: " + ex.getClass().getSimpleName() + " - " + ex.getMessage()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "error", "Internal Server Error",
                        "message", "Đã xảy ra lỗi hệ thống. Vui lòng thử lại sau."
                ));
    }
}
