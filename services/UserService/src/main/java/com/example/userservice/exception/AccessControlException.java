package com.example.userservice.exception;

public class AccessControlException extends RuntimeException {

    public AccessControlException(String message) {
        super(message);
    }
}
