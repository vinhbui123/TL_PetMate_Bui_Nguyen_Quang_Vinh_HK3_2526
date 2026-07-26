package com.petmate.server.config;

import com.petmate.server.service.SystemLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final SystemLogService systemLogService;

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, Object> response = new HashMap<>();
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        String mainMessage = "Dữ liệu không hợp lệ";
        if (!errors.isEmpty()) {
            mainMessage = String.join("\n", errors.values());
        }
        
        response.put("errors", errors);
        response.put("message", mainMessage);
        
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatusException(ResponseStatusException ex, WebRequest request) {
        log.warn("[GlobalExceptionHandler] API Error {}: {}", ex.getStatusCode(), ex.getReason());
        
        return ResponseEntity.status(ex.getStatusCode())
                .body(Map.of(
                        "error", ex.getStatusCode().toString(),
                        "message", ex.getReason() != null ? ex.getReason() : "Lỗi hệ thống"
                ));
    }

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

