package com.groupvm.catalog.resource;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class RequestCorrelationFilterTest {
    private final RequestCorrelationFilter filter = new RequestCorrelationFilter();

    @Test
    void createsServerIdAndClearsMdcAfterRequest() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/v1/products");
        request.addHeader(RequestCorrelationFilter.HEADER, "client-controlled");
        var response = new MockHttpServletResponse();
        var inChain = new AtomicReference<String>();

        filter.doFilter(request, response, (req, res) ->
                inChain.set(MDC.get("correlationId")));

        String id = response.getHeader(RequestCorrelationFilter.HEADER);
        assertNotNull(id);
        assertEquals(id, UUID.fromString(id).toString());
        assertNotEquals("client-controlled", id);
        assertEquals(id, inChain.get());
        assertEquals(id, request.getAttribute(RequestCorrelationFilter.ATTRIBUTE));
        assertNull(MDC.get("correlationId"));
    }

    @Test
    void logsFailureWithoutQueryTokenOrPayloadAndClearsMdc() throws Exception {
        var logger = (Logger) LoggerFactory.getLogger(RequestCorrelationFilter.class);
        var appender = new ListAppender<ILoggingEvent>();
        appender.start();
        logger.addAppender(appender);
        try {
            var request = new MockHttpServletRequest("POST", "/api/v1/admin/products");
            request.setQueryString("secret-query=private-value");
            request.addHeader("Authorization", "Bearer private-token");
            var response = new MockHttpServletResponse();
            filter.doFilter(request, response, (req, res) ->
                    ((MockHttpServletResponse) res).setStatus(403));

            assertEquals(1, appender.list.size());
            String message = appender.list.getFirst().getFormattedMessage();
            assertTrue(message.contains(response.getHeader(RequestCorrelationFilter.HEADER)));
            assertTrue(message.contains("status=403"));
            assertFalse(message.contains("private-value"));
            assertFalse(message.contains("private-token"));
            assertFalse(message.contains("/api/v1/admin/products"));
            assertNull(MDC.get("correlationId"));
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    @Test
    void logsUncaughtFailureAndClearsMdc() throws Exception {
        var logger = (Logger) LoggerFactory.getLogger(RequestCorrelationFilter.class);
        var appender = new ListAppender<ILoggingEvent>();
        appender.start();
        logger.addAppender(appender);
        try {
            var request = new MockHttpServletRequest("GET", "/api/v1/products");
            var response = new MockHttpServletResponse();
            org.junit.jupiter.api.Assertions.assertThrows(IOException.class,
                    () -> filter.doFilter(request, response, (req, res) -> {
                        throw new IOException("private exception detail");
                    }));
            assertEquals(1, appender.list.size());
            String message = appender.list.getFirst().getFormattedMessage();
            assertTrue(message.contains(response.getHeader(RequestCorrelationFilter.HEADER)));
            assertTrue(message.contains("status=500"));
            assertFalse(message.contains("private exception detail"));
            assertNull(MDC.get("correlationId"));
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }
}
