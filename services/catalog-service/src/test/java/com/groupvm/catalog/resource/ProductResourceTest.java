package com.groupvm.catalog.resource;

import com.groupvm.catalog.application.ProductService;
import com.groupvm.catalog.application.exception.DuplicateSkuException;
import com.groupvm.catalog.application.exception.ProductNotFoundException;
import com.groupvm.catalog.domain.Product;
import com.groupvm.catalog.domain.exception.InvalidTransitionException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductResource.class)
@ActiveProfiles("test")
@Import(CatalogSecurity.class)
@WithMockUser(authorities = "catalog_admin")
class ProductResourceTest {
    private static final String ADMIN = "/api/v1/admin/products";
    private static final String CREATE = """
            {"sku":" CHAIR-01 ","name":"Chair","description":"Wooden chair",
             "price":{"currency":"BRL","amount":"129.90"},
             "weightGrams":4500,"lengthCm":55,"widthCm":50,"heightCm":90}
            """;

    @Autowired MockMvc mvc;
    @MockBean ProductService service;

    @Test
    void createsDraftWithLocationAndAdminResponse() throws Exception {
        Product product = product();
        when(service.create(any(), eq("user"))).thenReturn(product);
        mvc.perform(post(ADMIN).contentType(MediaType.APPLICATION_JSON).content(CREATE))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", ADMIN + "/" + product.getId()))
                .andExpect(jsonPath("$.status", is("DRAFT")))
                .andExpect(jsonPath("$.sku", is("CHAIR-01")))
                .andExpect(jsonPath("$.price.currency", is("BRL")))
                .andExpect(jsonPath("$.price.amount", is("129.90")))
                .andExpect(jsonPath("$.weightGrams", is(4500)))
                .andExpect(jsonPath("$.priceHistory").isArray());
        verify(service).create(argThat(command -> command.sku().equals(" CHAIR-01 ")
                && command.price().compareTo(new BigDecimal("129.90")) == 0), eq("user"));
    }

    @Test
    void allowsPaddedSkuAndNameWhenTrimmedValuesFitLimits() throws Exception {
        Product product = product();
        when(service.create(any(), eq("user"))).thenReturn(product);
        String sku = " " + "S".repeat(80) + " ";
        String name = " " + "N".repeat(160) + " ";
        mvc.perform(post(ADMIN).contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE.replace(" CHAIR-01 ", sku).replace("Chair", name)))
                .andExpect(status().isCreated());
        verify(service).create(argThat(command -> command.sku().equals(sku)
                && command.name().equals(name)), eq("user"));
    }

    @Test
    void listsAndReadsAdministrativeProducts() throws Exception {
        Product product = product();
        when(service.list(0, 20)).thenReturn(new PageImpl<>(List.of(product), PageRequest.of(0, 20), 1));
        when(service.get(product.getId())).thenReturn(product);
        mvc.perform(get(ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].sku", is("CHAIR-01")))
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.size", is(20)))
                .andExpect(jsonPath("$.totalElements", is(1)));
        mvc.perform(get(ADMIN + "/{id}", product.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priceHistory").isEmpty());
    }

    @Test
    void patchCanClearDescriptionWithoutReplacingOtherFields() throws Exception {
        Product product = product();
        when(service.update(eq(product.getId()), any(), eq("user"))).thenReturn(product);
        mvc.perform(patch(ADMIN + "/{id}", product.getId())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"description\":null}"))
                .andExpect(status().isOk());
        verify(service).update(eq(product.getId()), argThat(command -> {
            assertTrue(command.descriptionPresent());
            assertNull(command.description());
            assertNull(command.name());
            return true;
        }), eq("user"));
    }

    @Test
    void activatesAndArchivesWithResponseBody() throws Exception {
        Product product = product();
        product.activate();
        when(service.activate(product.getId())).thenReturn(product);
        mvc.perform(post(ADMIN + "/{id}/activate", product.getId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status", is("ACTIVE")));
        product.archive();
        when(service.archive(product.getId())).thenReturn(product);
        mvc.perform(post(ADMIN + "/{id}/archive", product.getId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status", is("ARCHIVED")));
    }

    @Test
    void rejectsUnexpectedFieldsAndWrongJsonTypesBeforeService() throws Exception {
        String[] invalid = {
                CREATE.replace("\"weightGrams\":4500", "\"weightGrams\":\"4500\""),
                CREATE.replace("\"amount\":\"129.90\"", "\"amount\":129.90"),
                CREATE.replace("\"BRL\"", "\"USD\""),
                CREATE.replace("\"amount\":\"129.90\"", "\"amount\":\"129.999\""),
                CREATE.replace("\"heightCm\":90", "\"heightCm\":90,\"actor\":\"forged\""),
                CREATE.replace("\"amount\":\"129.90\"", "\"amount\":\"129.90\",\"actor\":\"forged\""),
                "[]"
        };
        for (String json : invalid) {
            mvc.perform(post(ADMIN).contentType(MediaType.APPLICATION_JSON).content(json))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(service);
    }

    @Test
    void rejectsNullCharacterInProductTextBeforeService() throws Exception {
        String escapedNul = "\\" + "u0000";
        Product product = product();
        UUID id = product.getId();
        when(service.create(any(), eq("user"))).thenReturn(product);
        when(service.update(eq(id), any(), eq("user"))).thenReturn(product);

        for (String field : List.of("sku", "name", "description")) {
            String original = field.equals("sku") ? " CHAIR-01 "
                    : field.equals("name") ? "Chair" : "Wooden chair";
            String invalidCreate = CREATE.replace("\"" + field + "\":\"" + original + "\"",
                    "\"" + field + "\":\"bad" + escapedNul + "value\"");
            mvc.perform(post(ADMIN).contentType(MediaType.APPLICATION_JSON).content(invalidCreate))
                    .andExpect(status().isBadRequest());
            if (!field.equals("sku")) {
                String invalidPatch = "{\"" + field + "\":\"bad" + escapedNul + "value\"}";
                mvc.perform(patch(ADMIN + "/{id}", id)
                                .contentType(MediaType.APPLICATION_JSON).content(invalidPatch))
                        .andExpect(status().isBadRequest());
            }
        }
        verifyNoInteractions(service);
    }

    @Test
    void rejectsEmptyPatchSkuChangeAndNullRequiredValues() throws Exception {
        UUID id = UUID.randomUUID();
        for (String json : List.of("{}", "{\"sku\":\"NEW\"}", "{\"name\":null}",
                "{\"price\":null}", "{\"weightGrams\":null}", "{\"price\":{\"currency\":\"BRL\",\"amount\":\"0.00\"}}")) {
            mvc.perform(patch(ADMIN + "/{id}", id).contentType(MediaType.APPLICATION_JSON).content(json))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(service);
    }

    @Test
    void rejectsInvalidPagingAndProductId() throws Exception {
        when(service.list(-1, 20)).thenThrow(new IllegalArgumentException("Invalid page or size"));
        when(service.list(0, 101)).thenThrow(new IllegalArgumentException("Invalid page or size"));
        mvc.perform(get(ADMIN).param("page", "-1")).andExpect(status().isBadRequest());
        mvc.perform(get(ADMIN).param("size", "101")).andExpect(status().isBadRequest());
        mvc.perform(get(ADMIN + "/not-a-uuid")).andExpect(status().isBadRequest());
        verify(service, times(2)).list(anyInt(), anyInt());
        verifyNoMoreInteractions(service);
    }

    @Test
    void usesSpecificProblemCodesWithoutLeakingDatabaseDetails() throws Exception {
        when(service.create(any(), eq("user"))).thenThrow(new DuplicateSkuException("CHAIR-01"));
        mvc.perform(post(ADMIN).contentType(MediaType.APPLICATION_JSON).content(CREATE))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code", is("duplicate_sku")))
                .andExpect(jsonPath("$.type", is("https://github.com/Ivictors/group-vm/issues/35#problem-duplicate-sku")));
        when(service.activate(any())).thenThrow(new InvalidTransitionException("Only complete draft products can be activated"));
        mvc.perform(post(ADMIN + "/{id}/activate", UUID.randomUUID()))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code", is("invalid_transition")))
                .andExpect(jsonPath("$.type", is("https://github.com/Ivictors/group-vm/issues/35#problem-invalid-transition")));
    }

    @Test
    void mapsConcurrentAndDatabaseSkuConflictsWithoutLeakingSql() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.archive(id)).thenThrow(new OptimisticLockingFailureException("internal-row-version"));
        mvc.perform(post(ADMIN + "/{id}/archive", id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("concurrent_update")))
                .andExpect(jsonPath("$.type", is("https://github.com/Ivictors/group-vm/issues/35#problem-concurrent-update")))
                .andExpect(jsonPath("$.detail", is("The product changed during this request")));
        when(service.create(any(), eq("user"))).thenThrow(new DataIntegrityViolationException("internal SQL",
                new SQLException("duplicate key on uk_products_sku_ci", "23505")));
        mvc.perform(post(ADMIN).contentType(MediaType.APPLICATION_JSON).content(CREATE))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("duplicate_sku")))
                .andExpect(jsonPath("$.detail", is("The SKU is already in use")));
        doThrow(new DataIntegrityViolationException("internal SQL",
                new SQLException("sensitive SQL detail", "23514"))).when(service).create(any(), eq("user"));
        mvc.perform(post(ADMIN).contentType(MediaType.APPLICATION_JSON).content(CREATE))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail", is("The request could not be completed")));
    }

    @Test
    void legacyPublicWriteRouteDoesNotExposeAdminOperations() throws Exception {
        mvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content(CREATE))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    void applicationErrorsCarryTheResponseCorrelationIdWithoutInternalMessages() throws Exception {
        assertCorrelated(mvc.perform(post(ADMIN).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":"))
                .andExpect(status().isBadRequest()).andReturn().getResponse());

        UUID missing = UUID.randomUUID();
        when(service.get(missing)).thenThrow(new ProductNotFoundException(missing));
        assertCorrelated(mvc.perform(get(ADMIN + "/{id}", missing))
                .andExpect(status().isNotFound()).andReturn().getResponse());

        when(service.create(any(), eq("user"))).thenThrow(new DuplicateSkuException("CHAIR-01"));
        assertCorrelated(mvc.perform(post(ADMIN).contentType(MediaType.APPLICATION_JSON).content(CREATE))
                .andExpect(status().isConflict()).andReturn().getResponse());

        when(service.create(any(), eq("user"))).thenThrow(new IllegalStateException("secret-internal-detail"));
        var internal = mvc.perform(post(ADMIN).contentType(MediaType.APPLICATION_JSON).content(CREATE))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail", is("The request could not be completed")))
                .andReturn().getResponse();
        assertCorrelated(internal);
        org.junit.jupiter.api.Assertions.assertFalse(internal.getContentAsString().contains("secret-internal-detail"));
    }

    @Test
    void unsupportedRequestFormatRemainsUnsupportedMediaType() throws Exception {
        mvc.perform(post(ADMIN).contentType(MediaType.TEXT_PLAIN).content("product"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(header().exists("X-Correlation-ID"))
                .andExpect(jsonPath("$.correlationId").isNotEmpty());
        verifyNoInteractions(service);
    }

    private static void assertCorrelated(MockHttpServletResponse response) throws Exception {
        String id = response.getHeader(RequestCorrelationFilter.HEADER);
        org.junit.jupiter.api.Assertions.assertNotNull(id);
        org.junit.jupiter.api.Assertions.assertEquals(id, UUID.fromString(id).toString());
        org.junit.jupiter.api.Assertions.assertEquals(id,
                new com.fasterxml.jackson.databind.ObjectMapper().readTree(response.getContentAsString())
                        .get("correlationId").asText());
    }

    private static Product product() {
        return new Product("CHAIR-01", "Chair", "Wooden chair", new BigDecimal("129.90"),
                4500, BigDecimal.valueOf(55), BigDecimal.valueOf(50), BigDecimal.valueOf(90));
    }
}
