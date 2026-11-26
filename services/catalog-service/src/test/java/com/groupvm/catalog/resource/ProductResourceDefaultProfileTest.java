package com.groupvm.catalog.resource;

import com.groupvm.catalog.application.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductResource.class)
class ProductResourceDefaultProfileTest {
    @Autowired MockMvc mvc;
    @MockBean ProductService service;

    @Test
    void administrativeRouteIsUnavailableWithDefaultProfile() throws Exception {
        mvc.perform(get("/api/v1/admin/products")).andExpect(status().isNotFound());
    }
}
