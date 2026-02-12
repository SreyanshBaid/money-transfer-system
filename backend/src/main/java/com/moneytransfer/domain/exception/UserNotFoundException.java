package com.moneytransfer.domain.exception;

/**
 * Exception thrown when a user with the specified ID or username doesn't exist.
 */
public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String message) {
        super(message);
    }

    public UserNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public UserNotFoundException(Long userId) {
        super("User not found: " + userId);
    }
}
