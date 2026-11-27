package com.groupvm.catalog.resource;

import com.groupvm.catalog.application.PublicProductService;
import com.groupvm.catalog.application.exception.ProductNotFoundException;
import com.groupvm.catalog.domain.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PublicProductResource.class)
@Import(CatalogSecurity.class)
class PublicProductResourceTest {
    @Autowired MockMvc mvc;
    @MockBean PublicProductService service;

    @Test
    void listUsesPublicProjectionAndPagination() throws Exception {
        Product product = product();
        product.activate();
        when(service.list(1, 1)).thenReturn(new PageImpl<>(List.of(product), PageRequest.of(1, 1), 3));
        mvc.perform(get("/api/v1/products").param("page", "1").param("size", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].id", is(product.getId().toString())))
                .andExpect(jsonPath("$.items[0].name", is("Chair")))
                .andExpect(jsonPath("$.items[0].price.amount", is("129.90")))
                .andExpect(jsonPath("$.items[0].sku").doesNotExist())
                .andExpect(jsonPath("$.items[0].status").doesNotExist())
                .andExpect(jsonPath("$.items[0].createdAt").doesNotExist())
                .andExpect(jsonPath("$.items[0].priceHistory").doesNotExist())
                .andExpect(jsonPath("$.page", is(1))).andExpect(jsonPath("$.totalElements", is(3)))
                .andExpect(jsonPath("$.totalPages", is(3)));
    }

    @Test
    void detailContainsShippingFieldsButNoAdminFields() throws Exception {
        Product product = product();
        product.activate();
        when(service.get(product.getId())).thenReturn(product);
        mvc.perform(get("/api/v1/products/{id}", product.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(product.getId().toString())))
                .andExpect(jsonPath("$.description", is("Wooden chair")))
                .andExpect(jsonPath("$.weightGrams", is(4500)))
                .andExpect(jsonPath("$.lengthCm", is(55)))
                .andExpect(jsonPath("$.widthCm", is(50)))
                .andExpect(jsonPath("$.heightCm", is(90)))
                .andExpect(jsonPath("$.sku").doesNotExist())
                .andExpect(jsonPath("$.status").doesNotExist())
                .andExpect(jsonPath("$.updatedAt").doesNotExist())
                .andExpect(jsonPath("$.priceHistory").doesNotExist());
    }

    @Test
    void missingProductHasSameNotFoundResponse() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.get(id)).thenThrow(new ProductNotFoundException(id));
        mvc.perform(get("/api/v1/products/{id}", id))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.title", is("Product not found")));
    }

    @Test
    void validatesPageSizeAndId() throws Exception {
        when(service.list(-1, 20)).thenThrow(new IllegalArgumentException("Invalid page or size"));
        when(service.list(0, 101)).thenThrow(new IllegalArgumentException("Invalid page or size"));
        mvc.perform(get("/api/v1/products").param("page", "-1")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/products").param("size", "101")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/products/not-a-uuid")).andExpect(status().isBadRequest());
        verify(service, times(2)).list(anyInt(), anyInt());
    }

    private static Product product() {
        return new Product("CHAIR-01", "Chair", "Wooden chair", new BigDecimal("129.90"),
                4500, BigDecimal.valueOf(55), BigDecimal.valueOf(50), BigDecimal.valueOf(90));
    }
}
