package com.cas.tsas.common.web;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void uses_request_header_when_present() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse resp = new MockHttpServletResponse();
        req.addHeader("X-Correlation-Id", "abc-123");
        String[] mdcInsideFilter = new String[1];
        FilterChain chain = (r, s) -> mdcInsideFilter[0] = MDC.get("correlationId");

        filter.doFilter(req, resp, chain);

        assertThat(mdcInsideFilter[0]).isEqualTo("abc-123");
        assertThat(resp.getHeader("X-Correlation-Id")).isEqualTo("abc-123");
        assertThat(MDC.get("correlationId")).isNull();
    }

    @Test
    void generates_uuid_when_header_missing() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse resp = new MockHttpServletResponse();
        String[] mdcInsideFilter = new String[1];
        FilterChain chain = (r, s) -> mdcInsideFilter[0] = MDC.get("correlationId");

        filter.doFilter(req, resp, chain);

        assertThat(mdcInsideFilter[0]).isNotBlank();
        assertThat(resp.getHeader("X-Correlation-Id")).isEqualTo(mdcInsideFilter[0]);
        assertThat(MDC.get("correlationId")).isNull();
    }

    @Test
    void cleans_mdc_on_exception() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse resp = new MockHttpServletResponse();
        FilterChain chain = (r, s) -> { throw new RuntimeException("boom"); };

        try {
            filter.doFilter(req, resp, chain);
        } catch (RuntimeException ignored) {
            // expected
        }

        assertThat(MDC.get("correlationId")).isNull();
    }
}
