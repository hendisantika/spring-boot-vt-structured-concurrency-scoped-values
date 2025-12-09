package id.my.hendisantika.springbootvtstructuredconcurrencyscopedvalues.dto;

import id.my.hendisantika.springbootvtstructuredconcurrencyscopedvalues.entity.Order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
public record OrderResponse(
        Long id,
        String orderNumber,
        CustomerInfo customer,
        List<OrderItemResponse> items,
        BigDecimal totalAmount,
        String status,
        String shippingAddress,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String processedBy
) {
    public static OrderResponse from(Order order, String processedBy) {
        CustomerInfo customerInfo = new CustomerInfo(
                order.getCustomer().getId(),
                order.getCustomer().getFullName(),
                order.getCustomer().getEmail()
        );

        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> new OrderItemResponse(
                        item.getId(),
                        new ProductInfo(
                                item.getProduct().getId(),
                                item.getProduct().getName(),
                                item.getProduct().getSku()
                        ),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getSubtotal()
                ))
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                customerInfo,
                itemResponses,
                order.getTotalAmount(),
                order.getStatus().name(),
                order.getShippingAddress(),
                order.getNotes(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                processedBy
        );
    }

    public record CustomerInfo(
            Long id,
            String fullName,
            String email
    ) {
    }

    public record OrderItemResponse(
            Long id,
            ProductInfo product,
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal subtotal
    ) {
    }

    public record ProductInfo(
            Long id,
            String name,
            String sku
    ) {
    }
}
