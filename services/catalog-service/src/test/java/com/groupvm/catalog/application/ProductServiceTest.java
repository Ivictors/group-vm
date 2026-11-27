package com.groupvm.catalog.application;

import com.groupvm.catalog.application.command.CreateProductCommand;
import com.groupvm.catalog.application.command.UpdateProductCommand;
import com.groupvm.catalog.application.exception.DuplicateSkuException;
import com.groupvm.catalog.application.exception.ProductNotFoundException;
import com.groupvm.catalog.domain.Product;
import com.groupvm.catalog.domain.ProductStatus;
import com.groupvm.catalog.domain.exception.InvalidProductException;
import com.groupvm.catalog.infra.ProductRepository;
import com.groupvm.catalog.infra.PriceHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class ProductServiceTest {
    private ProductRepository repository;
    private ProductService service;

    @BeforeEach
    void setUp() {
        repository = mock(ProductRepository.class);
        service = new ProductService(repository, mock(PriceHistoryRepository.class));
    }

    private CreateProductCommand createCommand(String sku) {
        return new CreateProductCommand(sku, "Chair", null, new BigDecimal("10.00"), 100,
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE);
    }

    @Test
    void createsDraftWithNormalizedSkuWhenAvailable() {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        Product product = service.create(createCommand(" sku-1 "), "operator");
        assertEquals("sku-1", product.getSku());
        assertEquals(ProductStatus.DRAFT, product.getStatus());
        verify(repository).existsBySkuIgnoreCase("sku-1");
        verify(repository).save(any());
    }

    @Test
    void rejectsCaseInsensitiveDuplicateWithoutSaving() {
        when(repository.existsBySkuIgnoreCase(argThat(sku -> sku.equalsIgnoreCase("sku-1")))).thenReturn(true);
        assertThrows(DuplicateSkuException.class, () -> service.create(createCommand(" SKU-1 "), "operator"));
        verify(repository).existsBySkuIgnoreCase("SKU-1");
        verify(repository, never()).save(any());
    }

    @Test
    void rejectsNullCharacterBeforeRepositoryAccess() {
        assertThrows(InvalidProductException.class, () -> service.create(
                new CreateProductCommand("SKU", "Cha\u0000ir", null, new BigDecimal("10.00"), 100,
                        BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE), "operator"));
        verifyNoInteractions(repository);
    }

    @Test
    void adminListUsesBoundedPageAndStableNewestFirstOrder() {
        when(repository.findAll(any(Pageable.class))).thenReturn(Page.empty());

        service.list(2, 25);

        verify(repository).findAll(argThat((Pageable pageable) ->
                pageable.getPageNumber() == 2
                        && pageable.getPageSize() == 25
                        && pageable.getSort().getOrderFor("createdAt") != null
                        && pageable.getSort().getOrderFor("createdAt").getDirection() == Sort.Direction.DESC
                        && pageable.getSort().getOrderFor("id") != null
                        && pageable.getSort().getOrderFor("id").getDirection() == Sort.Direction.DESC));
    }

    @Test
    void adminListRejectsInvalidBoundsBeforeQuery() {
        assertThrows(IllegalArgumentException.class, () -> service.list(-1, 20));
        assertThrows(IllegalArgumentException.class, () -> service.list(0, 0));
        assertThrows(IllegalArgumentException.class, () -> service.list(0, 101));
        verify(repository, never()).findAll(any(Pageable.class));
    }

    @Test
    void missingProductIsNotFound() {
        when(repository.findById(any())).thenReturn(Optional.empty());
        assertThrows(ProductNotFoundException.class, () -> service.get(UUID.randomUUID()));
    }

    @Test
    void updatesOnlySuppliedFieldsAndActivatesThenArchives() {
        Product product = new Product("SKU", "Chair", null, new BigDecimal("10.00"), 100,
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE);
        when(repository.findById(product.getId())).thenReturn(Optional.of(product));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Product updated = service.update(product.getId(), new UpdateProductCommand("Table", null, false,
                null, null, null, null, null), "operator");
        assertEquals("Table", updated.getName());
        assertEquals(new BigDecimal("10.00"), updated.getPrice());
        assertEquals(ProductStatus.ACTIVE, service.activate(product.getId()).getStatus());
        assertEquals(ProductStatus.ARCHIVED, service.archive(product.getId()).getStatus());
    }
}
