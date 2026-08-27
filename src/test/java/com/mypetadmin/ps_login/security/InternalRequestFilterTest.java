package com.mypetadmin.ps_login.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class InternalRequestFilterTest {

    private final InternalRequestFilter filter = new InternalRequestFilter("test-internal-key");

    @Test
    void rotaPublicaNaoExigeChaveInterna() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/version");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void rotaInternaSemChaveRetornaUnauthorized() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal/test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain, never()).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void rotaInternaComChaveInvalidaRetornaUnauthorized() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal/test");
        request.addHeader("X-Internal-Key", "wrong-key");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain, never()).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void rotaInternaComChaveValidaSegueFluxo() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal/test");
        request.addHeader("X-Internal-Key", "test-internal-key");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }
}
