package com.rikkeibank.notification.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rikkeibank.notification.entity.Notification;
import com.rikkeibank.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Event-driven / asynchronous processing (per SRS): consumes "transaction-events" published by
 * transaction-service and turns each one into a customer-facing notification, completely decoupled
 * from the transfer request/response cycle (the customer's transfer call already returned before
 * this listener even runs).
 */
@Component
@RequiredArgsConstructor
public class TransactionEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(TransactionEventConsumer.class);

    private final NotificationRepository notificationRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "transaction-events", groupId = "notification-service-group")
    public void onTransactionEvent(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            String eventType = node.path("eventType").asText();
            long customerId = node.path("customerId").asLong(-1);
            String amount = node.path("amount").asText();
            String reference = node.path("reference").asText();
            String reason = node.path("reason").asText("");

            String message = switch (eventType) {
                case "TRANSACTION_COMPLETED" ->
                        "Chuyen khoan thanh cong: " + amount + " VND (ref: " + reference + ")";
                case "TRANSACTION_FAILED" ->
                        "Chuyen khoan that bai (ref: " + reference + "): " + reason;
                case "TRANSACTION_COMPENSATED" ->
                        "Chuyen khoan bi huy va da hoan tien (ref: " + reference + "): " + reason;
                default -> "Su kien giao dich: " + eventType + " (ref: " + reference + ")";
            };

            Notification notification = Notification.builder()
                    .customerId(customerId == -1 ? null : customerId)
                    .eventType(eventType)
                    .message(message)
                    .build();
            notificationRepository.save(notification);
            log.info("Notification stored for customer {}: {}", customerId, message);
        } catch (Exception e) {
            log.error("Failed to process transaction event: {}", e.getMessage());
        }
    }
}
