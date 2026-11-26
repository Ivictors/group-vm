package com.groupvm.catalog.application;

import com.groupvm.catalog.application.exception.ProductNotFoundException;
import com.groupvm.catalog.domain.Product;
import com.groupvm.catalog.domain.ProductStatus;
import com.groupvm.catalog.infra.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class PublicProductServiceTest {
    private final ProductRepository repository = mock(ProductRepository.class);
    private final PublicProductService service = new PublicProductService(repository);

    @Test
    void listsOnlyActiveProductsWithBoundedDeterministicPage() {
        when(repository.findByStatus(eq(ProductStatus.ACTIVE), any(Pageable.class))).thenReturn(Page.empty());
        service.list(2, 25);
        verify(repository).findByStatus(eq(ProductStatus.ACTIVE), argThat((Pageable pageable) ->
                pageable.getPageNumber() == 2 && pageable.getPageSize() == 25
                        && pageable.getSort().equals(Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")))));
        verify(repository, never()).findAll(any(Pageable.class));
    }

    @Test
    void rejectsInvalidPagesBeforeQuery() {
        assertThrows(IllegalArgumentException.class, () -> service.list(-1, 20));
        assertThrows(IllegalArgumentException.class, () -> service.list(0, 0));
        assertThrows(IllegalArgumentException.class, () -> service.list(0, 101));
        verifyNoInteractions(repository);
    }

    @Test
    void readsOnlyActiveProductByIdAndTreatsMissingAsNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.findByIdAndStatus(id, ProductStatus.ACTIVE)).thenReturn(Optional.empty());
        assertThrows(ProductNotFoundException.class, () -> service.get(id));
        verify(repository).findByIdAndStatus(id, ProductStatus.ACTIVE);
        verify(repository, never()).findById(id);
    }

    @Test
    void returnsActiveProductFromFilteredQuery() {
        Product product = new Product("SKU", "Chair", null, new BigDecimal("10.00"), 100,
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE);
        product.activate();
        when(repository.findByIdAndStatus(product.getId(), ProductStatus.ACTIVE))
                .thenReturn(Optional.of(product));

        assertSame(product, service.get(product.getId()));
        verify(repository).findByIdAndStatus(product.getId(), ProductStatus.ACTIVE);
    }
}
