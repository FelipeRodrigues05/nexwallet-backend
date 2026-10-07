package com.spring.bank.transfer.messaging;

import com.spring.bank.common.config.messaging.RabbitMQConfig;
import com.spring.bank.transfer.service.CompleteTransferService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "nexwallet.runtime.role", havingValue = "api", matchIfMissing = true)
@RequiredArgsConstructor
public class TransferResponseConsumer {

    private static final Logger log = LoggerFactory.getLogger(TransferResponseConsumer.class);

    private final CompleteTransferService completeTransferService;

    @RabbitListener(queues = RabbitMQConfig.TRANSFER_RESPONSE_QUEUE)
    public void consume(Long transferId) {
        try {
            completeTransferService.complete(transferId);
        } catch (Exception error) {
            log.error("Failed to complete transfer id={}: {}", transferId, error.getMessage(), error);
            throw error;
        }
    }
}
