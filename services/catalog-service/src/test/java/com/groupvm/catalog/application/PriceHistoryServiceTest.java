package com.groupvm.catalog.application;

import com.groupvm.catalog.application.command.CreateProductCommand;
import com.groupvm.catalog.application.command.UpdateProductCommand;
import com.groupvm.catalog.domain.Product;
import com.groupvm.catalog.infra.PriceHistoryRepository;
import com.groupvm.catalog.infra.ProductRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PriceHistoryServiceTest {
    private final ProductRepository products = mock(ProductRepository.class);
    private final PriceHistoryRepository history = mock(PriceHistoryRepository.class);
    private final ProductService service = new ProductService(products, history);

    @Test
    void recordsInitialPriceWithValidatedActor() {
        when(products.save(any())).thenAnswer(call -> call.getArgument(0));
        Product product = service.create(command(), "operator-1");
        verify(history).save(argThat(entry -> entry.getProductId().equals(product.getId())
                && entry.getAmount().equals(new BigDecimal("10.00"))
                && entry.getActor().equals("operator-1")));
    }

    @Test
    void recordsOnlyActualPriceChanges() {
        Product product = new Product("SKU", "Chair", null, new BigDecimal("10.00"),
                100, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE);
        when(products.findById(product.getId())).thenReturn(Optional.of(product));
        when(products.save(any())).thenAnswer(call -> call.getArgument(0));
        service.update(product.getId(), update("10.00"), "operator-1");
        verifyNoInteractions(history);
        service.update(product.getId(), update("11.00"), "operator-2");
        verify(history).save(argThat(entry -> entry.getAmount().equals(new BigDecimal("11.00"))
                && entry.getActor().equals("operator-2")));
        assertEquals(new BigDecimal("11.00"), product.getPrice());
    }

    @Test
    void rejectsMissingActorBeforeMutation() {
        assertThrows(IllegalArgumentException.class, () -> service.create(command(), " "));
        assertThrows(IllegalArgumentException.class,
                () -> service.update(java.util.UUID.randomUUID(), update("11.00"), null));
        verifyNoInteractions(products, history);
    }

    private static CreateProductCommand command() {
        return new CreateProductCommand("SKU", "Chair", null, new BigDecimal("10.00"),
                100, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE);
    }

    private static UpdateProductCommand update(String price) {
        return new UpdateProductCommand(null, null, false, new BigDecimal(price),
                null, null, null, null);
    }
}
