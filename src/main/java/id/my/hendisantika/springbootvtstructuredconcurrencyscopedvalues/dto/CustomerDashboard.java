package id.my.hendisantika.springbootvtstructuredconcurrencyscopedvalues.dto;

import java.math.BigDecimal;
import java.util.List;

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
 * Aggregated dashboard data for a customer.
 * This is built using Structured Concurrency to fetch multiple
 * pieces of data in parallel.
 */
public record CustomerDashboard(
        CustomerDetails customer,
        List<RecentOrder> recentOrders,
        CustomerStats stats,
        List<ProductRecommendation> recommendations,
        String requestId,
        long fetchTimeMs
) {
    public record CustomerDetails(
            Long id,
            String fullName,
            String email,
            String phoneNumber,
            String shippingAddress
    ) {
    }

    public record RecentOrder(
            Long id,
            String orderNumber,
            String status,
            BigDecimal totalAmount,
            String createdAt,
            int itemCount
    ) {
    }

    public record CustomerStats(
            long totalOrders,
            BigDecimal totalSpent,
            BigDecimal averageOrderValue,
            String memberSince
    ) {
    }

    public record ProductRecommendation(
            Long id,
            String name,
            String category,
            BigDecimal price,
            String reason
    ) {
    }
}
