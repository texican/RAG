package com.enterprise.rag.shared.exception;

public abstract class RagException extends RuntimeException {
    
    private final String errorCode;
    
    protected RagException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    protected RagException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}