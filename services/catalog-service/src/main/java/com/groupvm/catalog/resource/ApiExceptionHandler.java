package com.groupvm.catalog.resource;

import com.groupvm.catalog.application.exception.DuplicateSkuException;
import com.groupvm.catalog.application.exception.ProductNotFoundException;
import com.groupvm.catalog.domain.exception.ArchivedProductException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ProductNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ProblemDetail notFound(ProductNotFoundException exception) { return problem(HttpStatus.NOT_FOUND, "Product not found", exception.getMessage()); }
    @ExceptionHandler({DuplicateSkuException.class, DataIntegrityViolationException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    ProblemDetail conflict(Exception exception) {
        var detail = exception instanceof DuplicateSkuException
                ? exception.getMessage()
                : "The product conflicts with an existing record";
        return problem(HttpStatus.CONFLICT, "Product conflict", detail);
    }
    @ExceptionHandler(ArchivedProductException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    ProblemDetail archived(ArchivedProductException exception) { return problem(HttpStatus.CONFLICT, "Product is archived", exception.getMessage()); }
    private ProblemDetail problem(HttpStatus status, String title, String detail) {
        var problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
