package com.ifchange.tob.common.cache;

public class CacheException extends RuntimeException {
    public CacheException(String message) {
        super(message);
    }

    @SuppressWarnings("unused")
    public CacheException(String message, Throwable cause) {
        super(message, cause);
    }
}
