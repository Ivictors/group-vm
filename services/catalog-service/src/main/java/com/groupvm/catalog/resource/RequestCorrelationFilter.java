package com.groupvm.catalog.resource;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestCorrelationFilter extends OncePerRequestFilter {
    static final String HEADER = "X-Correlation-ID";
    static final String ATTRIBUTE = RequestCorrelationFilter.class.getName() + ".id";
    private static final Logger LOG = LoggerFactory.getLogger(RequestCorrelationFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String id = UUID.randomUUID().toString();
        request.setAttribute(ATTRIBUTE, id);
        response.setHeader(HEADER, id);
        MDC.put("correlationId", id);
        long started = System.nanoTime();
        boolean failed = false;
        try {
            chain.doFilter(request, response);
        } catch (ServletException | IOException | RuntimeException exception) {
            failed = true;
            logFailure(id, request, 500, started);
            throw exception;
        } finally {
            try {
                if (!failed && response.getStatus() >= 400) {
                    logFailure(id, request, response.getStatus(), started);
                }
            } finally {
                MDC.remove("correlationId");
            }
        }
    }

    private static void logFailure(String id, HttpServletRequest request, int status, long started) {
        LOG.warn("Request failed correlationId={} method={} status={} durationMs={}",
                id, request.getMethod(), status, (System.nanoTime() - started) / 1_000_000);
    }

    static String id(HttpServletRequest request) {
        return (String) request.getAttribute(ATTRIBUTE);
    }
}
