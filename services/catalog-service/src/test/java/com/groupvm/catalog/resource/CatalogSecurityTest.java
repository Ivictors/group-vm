package com.groupvm.catalog.resource;

import com.groupvm.catalog.application.ProductService;
import com.groupvm.catalog.application.PublicProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.mockito.Mockito.when;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import org.springframework.http.MediaType;

@WebMvcTest({ProductResource.class, PublicProductResource.class})
@Import(CatalogSecurity.class)
class CatalogSecurityTest {
    @Autowired MockMvc mvc;
    @MockBean ProductService adminService;
    @MockBean PublicProductService publicService;

    @Test
    void publicReadsAreAnonymousAndWritesAreNot() throws Exception {
        when(publicService.list(0, 20)).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));
        mvc.perform(get("/api/v1/products")).andExpect(status().isOk());
        mvc.perform(post("/api/v1/products")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/products").with(jwt())).andExpect(status().isForbidden());
    }

    @Test
    void adminRequiresAuthenticatedCatalogRole() throws Exception {
        when(adminService.list(0, 20)).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));
        mvc.perform(get("/api/v1/admin/products")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/admin/products")).andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Unauthorized"));
        mvc.perform(get("/api/v1/admin/products").with(jwt())).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/products").with(jwt().authorities(new SimpleGrantedAuthority("catalog_admin"))))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/v1/admin/products/00000000-0000-0000-0000-000000000001")
                        .with(jwt().authorities(new SimpleGrantedAuthority("catalog_admin"))))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/products/unexpected/subroute")
                        .with(jwt().authorities(new SimpleGrantedAuthority("catalog_admin"))))
                .andExpect(status().isForbidden());
    }
}
