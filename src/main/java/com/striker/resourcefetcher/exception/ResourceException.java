package com.striker.resourcefetcher.exception;

/**
 * Represents and wraps any I/O error that may occur during usage of TempFolder and Resource API.
 * This exception is generally being throws when library doesn't have access to read/write/delete particular
 * file or folder. Also, exception is unchecked, so it does not require to wrap API usage to try catch, but
 * I/O exceptions will be provided as a cause if any.
 */
public class ResourceException extends RuntimeException {
    /**
     * Constructs new ResourceException with message only
     *
     * @param message error message
     */
    public ResourceException(String message) {
        super(message);
    }

    /**
     * Constructs new ResourceException with message and cause provided
     *
     * @param message error message
     * @param cause   {@link Throwable} cause of this exception
     */
    public ResourceException(String message, Throwable cause) {
        super(message, cause);
    }
}
