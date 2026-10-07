package com.spring.bank.domain.listener;

import com.spring.bank.common.config.messaging.RabbitMQConfig;
import com.spring.bank.domain.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "nexwallet.runtime.role", havingValue = "legacy-domain")
@RequiredArgsConstructor
public class TransactionListener {

    private static final Logger log = LoggerFactory.getLogger(TransactionListener.class);

    private final TransactionService transactionService;

    @RabbitListener(queues = RabbitMQConfig.TRANSACTION_RESPONSE_QUEUE)
    public void listenResponseQueue(Long transactionId) {
        try {
            this.transactionService.completeTransaction(transactionId);
        } catch (Exception e) {
            log.error("Failed to complete transaction id={}: {}", transactionId, e.getMessage(), e);
        }
    }
}
