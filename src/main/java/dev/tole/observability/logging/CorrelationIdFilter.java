package dev.tole.observability.logging;

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
import java.time.Instant;
import java.util.UUID;


@Component                              // Spring lo detecta y lo aplica a todas las URLs
@Order(Ordered.HIGHEST_PRECEDENCE)      // el primero de la cola: el id existe para todo lo demás
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final String CORRELATION_ID = "correlationId";
    private static final String HEADER = "X-Correlation-Id";
    private static final Logger log = LoggerFactory.getLogger(CorrelationIdFilter.class);


    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String correlationId = request.getHeader(HEADER);
        if (correlationId == null || correlationId.isBlank() || correlationId.length() > 64) {
            correlationId = UUID.randomUUID().toString();
        }
        MDC.put(CORRELATION_ID, correlationId);
        response.setHeader(HEADER, correlationId);
        long inicio = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            log.atInfo()
                    .addKeyValue("status", response.getStatus())
                    .addKeyValue("method", request.getMethod())
                    .addKeyValue("endpoint", request.getRequestURI())
                    .addKeyValue("duration_ms", (System.nanoTime() - inicio) / 1_000_000)
                    .log("Petición completada");
            MDC.remove(CORRELATION_ID);
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request)  {
        return (request.getRequestURI().startsWith("/actuator"));
    }
}
