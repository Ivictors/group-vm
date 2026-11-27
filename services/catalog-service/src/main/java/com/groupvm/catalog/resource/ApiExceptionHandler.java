package com.groupvm.catalog.resource;

import com.groupvm.catalog.application.exception.DuplicateSkuException;
import com.groupvm.catalog.application.exception.ProductNotFoundException;
import com.groupvm.catalog.domain.exception.InvalidProductException;
import com.groupvm.catalog.domain.exception.InvalidTransitionException;
import com.groupvm.catalog.resource.dto.InvalidProductRequestException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.sql.SQLException;
import java.net.URI;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ProductNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ProblemDetail notFound(ProductNotFoundException exception, HttpServletRequest request) {
        return problem(request, HttpStatus.NOT_FOUND, "Product not found", "Product not found", null);
    }

    @ExceptionHandler({InvalidProductRequestException.class, InvalidProductException.class,
            IllegalArgumentException.class, MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ProblemDetail invalidRequest(Exception exception, HttpServletRequest request) {
        return problem(request, HttpStatus.BAD_REQUEST, "Invalid product request", "The request is invalid", "invalid_request");
    }

    @ExceptionHandler(DuplicateSkuException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    ProblemDetail duplicateSku(DuplicateSkuException exception, HttpServletRequest request) {
        return problem(request, HttpStatus.CONFLICT, "Duplicate SKU", "The SKU is already in use", "duplicate_sku");
    }

    @ExceptionHandler(InvalidTransitionException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    ProblemDetail invalidTransition(InvalidTransitionException exception, HttpServletRequest request) {
        return problem(request, HttpStatus.CONFLICT, "Invalid product transition",
                "The product cannot make this transition", "invalid_transition");
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    ProblemDetail concurrentUpdate(OptimisticLockingFailureException exception, HttpServletRequest request) {
        return problem(request, HttpStatus.CONFLICT, "Concurrent update",
                "The product changed during this request", "concurrent_update");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ProblemDetail> dataConflict(DataIntegrityViolationException exception, HttpServletRequest request) {
        if (isSkuUniqueViolation(exception)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(problem(request, HttpStatus.CONFLICT, "Duplicate SKU", "The SKU is already in use", "duplicate_sku"));
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(problem(request, HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error",
                        "The request could not be completed", null));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    ProblemDetail unsupportedMediaType(HttpMediaTypeNotSupportedException exception, HttpServletRequest request) {
        return problem(request, HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported media type",
                "The request content type is not supported", null);
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    ResponseEntity<Void> notAcceptable(HttpMediaTypeNotAcceptableException exception) {
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    ProblemDetail unexpected(Exception exception, HttpServletRequest request) {
        return problem(request, HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error",
                "The request could not be completed", null);
    }

    private boolean isSkuUniqueViolation(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sql && "23505".equals(sql.getSQLState())
                    && sql.getMessage() != null
                    && (sql.getMessage().contains("uk_products_sku_ci")
                    || sql.getMessage().contains("uk_products_sku"))) {
                return true;
            }
        }
        return false;
    }

    private ProblemDetail problem(HttpServletRequest request, HttpStatus status,
                                  String title, String detail, String code) {
        var problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setProperty("correlationId", RequestCorrelationFilter.id(request));
        if (code != null) {
            problem.setProperty("code", code);
            if (status == HttpStatus.CONFLICT) {
                problem.setType(URI.create("https://github.com/Ivictors/group-vm/issues/35#problem-" +
                        code.replace('_', '-')));
            }
        }
        return problem;
    }
}
