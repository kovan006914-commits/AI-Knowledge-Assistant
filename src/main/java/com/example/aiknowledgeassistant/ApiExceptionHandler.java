package com.example.aiknowledgeassistant;

import com.example.aiknowledgeassistant.dto.ApiResponse;
import com.example.aiknowledgeassistant.exception.ApiProblem;
import com.example.aiknowledgeassistant.exception.InvalidCredentialsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ApiProblem.class)
    public ResponseEntity<ApiResponse> problem(ApiProblem error) {
        return ResponseEntity.status(error.status()).body(new ApiResponse(false, error.getMessage()));
    }

    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse> invalidParameter(Exception error) {
        return ResponseEntity.badRequest().body(new ApiResponse(false, "参数格式不正确"));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiResponse> invalidCredentials(InvalidCredentialsException error) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponse(false, error.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse> invalidInput(IllegalArgumentException error) {
        return ResponseEntity.badRequest().body(new ApiResponse(false, error.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse> invalidJson(HttpMessageNotReadableException error) {
        return ResponseEntity.badRequest().body(new ApiResponse(false, "请求正文不能为空，且必须是合法JSON"));
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ApiResponse> duplicate(DuplicateKeyException error) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiResponse(false, "用户名或邮箱已被使用"));
    }

    @ExceptionHandler({DataAccessException.class, IllegalStateException.class})
    public ResponseEntity<ApiResponse> storageError(Exception error) {
        log.error("用户操作失败", error);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "操作失败，请稍后重试"));
    }
}
