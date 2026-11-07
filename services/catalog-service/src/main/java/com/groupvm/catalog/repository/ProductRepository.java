package com.groupvm.catalog.repository;

import com.groupvm.catalog.entity.Product;

public interface ProductRepository {
    boolean existsBySkuIgnoreCase(String sku);

    Product save(Product product);
}
