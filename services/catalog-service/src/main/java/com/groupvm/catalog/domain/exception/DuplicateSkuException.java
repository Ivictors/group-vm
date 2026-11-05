package com.groupvm.catalog.domain.exception;

public class DuplicateSkuException extends RuntimeException {
    public DuplicateSkuException(String sku) {
        super("SKU is already in use: " + sku);
    }
}
