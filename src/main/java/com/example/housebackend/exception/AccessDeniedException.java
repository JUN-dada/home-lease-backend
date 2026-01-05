package com.example.housebackend.exception;

public class AccessDeniedException extends BusinessException {

    public AccessDeniedException(String message) {
        super(message);
    }
}
