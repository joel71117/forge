package com.forge.infrastructure.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class CorrelationIdFilterTest {
    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void preservesClientCorrelationIdAndCleansContext() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = (requestArg, responseArg) -> {
            assertEquals("checkout-42", MDC.get(CorrelationIdFilter.MDC_KEY));
        };
        when(request.getHeader(CorrelationIdFilter.HEADER)).thenReturn("checkout-42");

        filter.doFilter(request, response, chain);

        verify(response).setHeader(CorrelationIdFilter.HEADER, "checkout-42");
        assertNull(MDC.get(CorrelationIdFilter.MDC_KEY));
    }

    @Test
    void generatesResponseIdWhenHeaderIsBlank() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getHeader(CorrelationIdFilter.HEADER)).thenReturn("  ");

        filter.doFilter(request, response, (requestArg, responseArg) -> {
            assertNotNull(MDC.get(CorrelationIdFilter.MDC_KEY));
            assertTrue(MDC.get(CorrelationIdFilter.MDC_KEY).matches("[0-9a-f-]{36}"));
        });

        verify(response).setHeader(eq(CorrelationIdFilter.HEADER),
                org.mockito.ArgumentMatchers.matches("[0-9a-f-]{36}"));
    }

    @Test
    void clearsContextWhenDownstreamRequestFails() throws IOException, ServletException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getHeader(CorrelationIdFilter.HEADER)).thenReturn("failed-request");

        try {
            filter.doFilter(request, response, (requestArg, responseArg) -> {
                throw new ServletException("controller failed");
            });
        } catch (ServletException expected) {
            // Preserve the downstream failure while still testing cleanup.
        }

        assertNull(MDC.get(CorrelationIdFilter.MDC_KEY));
    }
}