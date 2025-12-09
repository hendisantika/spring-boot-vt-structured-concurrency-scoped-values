package id.my.hendisantika.springbootvtstructuredconcurrencyscopedvalues.context;

import lombok.Builder;

import java.time.Instant;

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
 * Immutable request context that is propagated through the request lifecycle
 * using ScopedValues. This context is automatically available in all virtual
 * threads spawned during request processing.
 */
@Builder
public record RequestContext(
        String requestId,
        String userId,
        String userEmail,
        String correlationId,
        Instant timestamp,
        String clientIp,
        String userAgent
) {
    public static RequestContext empty() {
        return RequestContext.builder()
                .requestId("unknown")
                .timestamp(Instant.now())
                .build();
    }

    public RequestContext withUserId(String userId) {
        return RequestContext.builder()
                .requestId(this.requestId)
                .userId(userId)
                .userEmail(this.userEmail)
                .correlationId(this.correlationId)
                .timestamp(this.timestamp)
                .clientIp(this.clientIp)
                .userAgent(this.userAgent)
                .build();
    }

    public RequestContext withUserEmail(String userEmail) {
        return RequestContext.builder()
                .requestId(this.requestId)
                .userId(this.userId)
                .userEmail(userEmail)
                .correlationId(this.correlationId)
                .timestamp(this.timestamp)
                .clientIp(this.clientIp)
                .userAgent(this.userAgent)
                .build();
    }
}
