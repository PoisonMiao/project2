package com.ifchange.tob.common.fastdfs;

public class FastDfsException extends RuntimeException {
    public FastDfsException(String message) {
        super(message);
    }

    public FastDfsException(String message, Throwable cause) {
        super(message, cause);
    }
}
