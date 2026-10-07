package com.spring.bank.transaction.messaging;

import com.spring.bank.common.config.messaging.RabbitMQConfig;
import com.spring.bank.transaction.service.CompleteTransactionService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "nexwallet.runtime.role", havingValue = "api", matchIfMissing = true)
@RequiredArgsConstructor
public class TransactionResponseConsumer {

    private static final Logger log = LoggerFactory.getLogger(TransactionResponseConsumer.class);

    private final CompleteTransactionService completeTransactionService;

    @RabbitListener(queues = RabbitMQConfig.TRANSACTION_RESPONSE_QUEUE)
    public void consume(Long transactionId) {
        try {
            completeTransactionService.complete(transactionId);
        } catch (Exception error) {
            log.error("Failed to complete transaction id={}: {}", transactionId, error.getMessage(), error);
            throw error;
        }
    }
}
