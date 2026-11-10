package com.groupvm.catalog.domain;
import com.groupvm.catalog.domain.exception.ArchivedProductException; import org.junit.jupiter.api.Test; import java.math.BigDecimal; import static org.junit.jupiter.api.Assertions.*;
class ProductTest { @Test void createsActiveProduct(){var p=new Product("SKU-1","Chair","desc",new BigDecimal("10.00"));assertNotNull(p.getId());assertFalse(p.isArchived());} @Test void archivedProductCannotBeUpdated(){var p=new Product("SKU-1","Chair",null,BigDecimal.TEN);p.archive();assertThrows(ArchivedProductException.class,()->p.update("Table",null,BigDecimal.ONE));} }
