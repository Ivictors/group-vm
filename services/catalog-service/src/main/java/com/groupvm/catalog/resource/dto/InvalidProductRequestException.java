package com.groupvm.catalog.resource.dto;

public class InvalidProductRequestException extends RuntimeException {
    public InvalidProductRequestException(String message) {
        super(message);
    }
}
