package com.tencent.tcvectordb.springboot.exception;

/**
 * 向量数据库自定义异常类
 *
 * @author Tencent Cloud VectorDB Team
 */
public class VectorDBException extends RuntimeException {

    private String errorCode;

    public VectorDBException(String message) {
        super(message);
    }

    public VectorDBException(String message, Throwable cause) {
        super(message, cause);
    }

    public VectorDBException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public VectorDBException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
