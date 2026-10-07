package com.spring.bank.domain.listener;

import com.spring.bank.common.config.messaging.RabbitMQConfig;
import com.spring.bank.domain.service.TransferService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "nexwallet.runtime.role", havingValue = "legacy-domain")
@RequiredArgsConstructor
public class TransferListener {

    private static final Logger log = LoggerFactory.getLogger(TransferListener.class);

    private final TransferService transferService;

    @RabbitListener(queues = RabbitMQConfig.TRANSFER_RESPONSE_QUEUE)
    public void listenResponseQueue(Long transferId) {
        try {
            this.transferService.completeTransfer(transferId);
        } catch (Exception e) {
            log.error("Failed to complete transfer id={}: {}", transferId, e.getMessage(), e);
        }
    }
}
