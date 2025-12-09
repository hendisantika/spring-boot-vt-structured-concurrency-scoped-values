package id.my.hendisantika.springbootvtstructuredconcurrencyscopedvalues.context;

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
 * Central holder for all ScopedValue instances used throughout the application.
 * <p>
 * ScopedValues (JEP 481, finalized in JDK 25) provide a mechanism to share
 * immutable data within and across threads. They are particularly useful with
 * Virtual Threads as they are automatically inherited by child threads.
 * <p>
 * Key benefits over ThreadLocal:
 * - Immutable values prevent accidental modifications
 * - Automatic inheritance in structured concurrency
 * - Better performance with Virtual Threads
 * - Clear scope boundaries
 */
public final class ScopedValues {

    /**
     * Holds the current request context for the duration of a request.
     * This is automatically propagated to all child virtual threads
     * created using StructuredTaskScope.
     */
    public static final ScopedValue<RequestContext> REQUEST_CONTEXT = ScopedValue.newInstance();
    /**
     * Holds the current transaction ID for distributed tracing.
     */
    public static final ScopedValue<String> TRANSACTION_ID = ScopedValue.newInstance();
    /**
     * Holds the current user's permission level for authorization checks.
     */
    public static final ScopedValue<String> USER_ROLE = ScopedValue.newInstance();

    private ScopedValues() {
        // Utility class - prevent instantiation
    }

    /**
     * Gets the current request context or throws if not bound.
     */
    public static RequestContext currentRequestContext() {
        if (REQUEST_CONTEXT.isBound()) {
            return REQUEST_CONTEXT.get();
        }
        return RequestContext.empty();
    }

    /**
     * Gets the current transaction ID or a default value.
     */
    public static String currentTransactionId() {
        return TRANSACTION_ID.isBound() ? TRANSACTION_ID.get() : "no-transaction";
    }

    /**
     * Gets the current user role or "anonymous".
     */
    public static String currentUserRole() {
        return USER_ROLE.isBound() ? USER_ROLE.get() : "anonymous";
    }

    /**
     * Checks if a request context is currently bound.
     */
    public static boolean hasRequestContext() {
        return REQUEST_CONTEXT.isBound();
    }
}
