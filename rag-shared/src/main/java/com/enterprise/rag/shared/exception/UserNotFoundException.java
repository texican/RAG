package com.enterprise.rag.shared.exception;

import java.util.UUID;

public class UserNotFoundException extends RagException {
    
    public UserNotFoundException(UUID userId) {
        super("User not found with ID: " + userId, "USER_NOT_FOUND");
    }
    
    public UserNotFoundException(String email) {
        super("User not found with email: " + email, "USER_NOT_FOUND");
    }
}