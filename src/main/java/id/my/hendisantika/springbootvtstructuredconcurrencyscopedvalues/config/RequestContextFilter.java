package id.my.hendisantika.springbootvtstructuredconcurrencyscopedvalues.config;

import id.my.hendisantika.springbootvtstructuredconcurrencyscopedvalues.context.RequestContext;
import id.my.hendisantika.springbootvtstructuredconcurrencyscopedvalues.context.ScopedValues;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

/**
 * Created by IntelliJ IDEA.
 * Project : spring-boot-vt-structured-concurrency-scoped-values
 * User: hendisantika
 * Link: s.id/hendisantika
 * Email: hendisantika@yahoo.co.id
 * Telegram : @hendisantika34
 * Date: 09/12/25
 * Time: 08.30
 * To change this template use File | Settings | File Templates.
 */

/**
 * Filter that establishes ScopedValue bindings for each HTTP request.
 * <p>
 * This filter runs early in the filter chain and uses ScopedValue.where().run()
 * to bind the RequestContext for the duration of the request. All code
 * executing within the request (including virtual threads spawned via
 * StructuredTaskScope) will have access to this context.
 */
@Slf4j
@Component
@Order(1)
public class RequestContextFilter implements Filter {

    private static final String HEADER_REQUEST_ID = "X-Request-ID";
    private static final String HEADER_CORRELATION_ID = "X-Correlation-ID";
    private static final String HEADER_USER_ID = "X-User-ID";
    private static final String HEADER_USER_EMAIL = "X-User-Email";

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;

        // Build the request context from headers
        RequestContext context = buildRequestContext(httpRequest);

        log.debug("Binding RequestContext for request: {} [correlationId={}]",
                context.requestId(), context.correlationId());

        // Use ScopedValue.where().run() to bind the context for the duration of the request (JDK 25 API)
        try {
            ScopedValue.where(ScopedValues.REQUEST_CONTEXT, context).run(() -> {
                try {
                    filterChain.doFilter(servletRequest, servletResponse);
                } catch (IOException | ServletException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (RuntimeException e) {
            if (e.getCause() instanceof IOException ioException) {
                throw ioException;
            }
            if (e.getCause() instanceof ServletException servletException) {
                throw servletException;
            }
            throw e;
        }

        log.debug("Request completed: {} [correlationId={}]",
                context.requestId(), context.correlationId());
    }

    private RequestContext buildRequestContext(HttpServletRequest request) {
        String requestId = getOrGenerateHeader(request, HEADER_REQUEST_ID);
        String correlationId = getOrGenerateHeader(request, HEADER_CORRELATION_ID);

        return RequestContext.builder()
                .requestId(requestId)
                .correlationId(correlationId)
                .userId(request.getHeader(HEADER_USER_ID))
                .userEmail(request.getHeader(HEADER_USER_EMAIL))
                .clientIp(getClientIp(request))
                .userAgent(request.getHeader("User-Agent"))
                .timestamp(Instant.now())
                .build();
    }

    private String getOrGenerateHeader(HttpServletRequest request, String headerName) {
        String value = request.getHeader(headerName);
        return (value != null && !value.isBlank()) ? value : UUID.randomUUID().toString();
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
