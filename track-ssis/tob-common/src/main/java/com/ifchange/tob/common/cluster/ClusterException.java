package com.ifchange.tob.common.cluster;

class ClusterException extends RuntimeException {
    ClusterException(String message) {
        super(message);
    }

    ClusterException(String message, Throwable cause) {
        super(message, cause);
    }
}
