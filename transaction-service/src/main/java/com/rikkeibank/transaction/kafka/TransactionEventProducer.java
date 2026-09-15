package com.rikkeibank.transaction.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Asynchronous, event-driven communication (per SRS): publishes domain events to Kafka so
 * notification-service (and any future subscriber) can react without transaction-service
 * knowing or caring who is listening (loose coupling).
 */
@Component
@RequiredArgsConstructor
public class TransactionEventProducer {

    private static final Logger log = LoggerFactory.getLogger(TransactionEventProducer.class);
    private static final String TOPIC = "transaction-events";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void publishCompleted(Long transactionId, Long fromAccountId, Long toAccountId,
                                  Long customerId, String amount, String reference) {
        publish("TRANSACTION_COMPLETED", transactionId, fromAccountId, toAccountId, customerId, amount, reference, null);
    }

    public void publishFailed(Long transactionId, Long fromAccountId, Long toAccountId,
                               Long customerId, String amount, String reference, String reason) {
        publish("TRANSACTION_FAILED", transactionId, fromAccountId, toAccountId, customerId, amount, reference, reason);
    }

    public void publishCompensated(Long transactionId, Long fromAccountId, Long toAccountId,
                                    Long customerId, String amount, String reference, String reason) {
        publish("TRANSACTION_COMPENSATED", transactionId, fromAccountId, toAccountId, customerId, amount, reference, reason);
    }

    private void publish(String eventType, Long transactionId, Long fromAccountId, Long toAccountId,
                          Long customerId, String amount, String reference, String reason) {
        try {
            Map<String, Object> event = Map.of(
                    "eventType", eventType,
                    "transactionId", transactionId,
                    "fromAccountId", fromAccountId,
                    "toAccountId", toAccountId,
                    "customerId", customerId == null ? -1 : customerId,
                    "amount", amount,
                    "reference", reference,
                    "reason", reason == null ? "" : reason,
                    "timestamp", System.currentTimeMillis()
            );
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(TOPIC, String.valueOf(transactionId), payload);
            log.info("Published Kafka event {} for transaction {}", eventType, transactionId);
        } catch (Exception e) {
            log.error("Failed to publish Kafka event for transaction {}: {}", transactionId, e.getMessage());
        }
    }
}
