package com.tencent.tcvectordb.springboot.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 * 统一处理应用中的各类异常,返回规范化的错误响应
 *
 * @author Tencent Cloud VectorDB Team
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理向量数据库相关异常
     */
    @ExceptionHandler(VectorDBException.class)
    public ResponseEntity<Map<String, Object>> handleVectorDBException(VectorDBException e) {
        log.error("向量数据库操作异常: {}", e.getMessage(), e);

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("errorCode", e.getErrorCode() != null ? e.getErrorCode() : "VECTORDB_ERROR");
        response.put("message", e.getMessage());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * 处理文件上传大小超限异常
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.error("文件上传大小超限: {}", e.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("errorCode", "FILE_TOO_LARGE");
        response.put("message", "上传文件大小超出限制");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 处理非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("非法参数: {}", e.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("errorCode", "INVALID_ARGUMENT");
        response.put("message", e.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 处理其他未预期的异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception e) {
        log.error("系统异常: {}", e.getMessage(), e);

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("errorCode", "SYSTEM_ERROR");
        response.put("message", "系统内部错误: " + e.getMessage());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
