package com.groupvm.catalog.application.port.out;

import com.groupvm.catalog.domain.Product;

public interface ProductRepository {
    boolean existsBySkuIgnoreCase(String sku);

    Product save(Product product);
}
