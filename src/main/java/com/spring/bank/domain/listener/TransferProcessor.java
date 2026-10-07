package com.spring.bank.domain.listener;

import com.spring.bank.common.config.messaging.RabbitMQConfig;
import com.spring.bank.common.exception.TransferNotFoundException;
import com.spring.bank.domain.dto.transaction.CreateTransactionDTO;
import com.spring.bank.domain.enums.transaction.TransactionTypeEnum;
import com.spring.bank.domain.model.Transfer;
import com.spring.bank.domain.repository.TransferRepository;
import com.spring.bank.domain.service.TransactionService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "nexwallet.runtime.role", havingValue = "legacy-domain")
@RequiredArgsConstructor
public class TransferProcessor {

    private static final Logger log = LoggerFactory.getLogger(TransferProcessor.class);

    private final TransferRepository transferRepository;
    private final TransactionService transactionService;
    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = RabbitMQConfig.TRANSFER_QUEUE)
    @Transactional
    public void process(Long transferId) {
        Transfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new TransferNotFoundException(
                        String.format("Transfer with ID %s not found", transferId)));

        try {
            transactionService.create(new CreateTransactionDTO(
                    transfer.getFromAccount(), TransactionTypeEnum.DEBIT,
                    transfer.getAmount(), transfer.getDescription()
            ));
            transactionService.create(new CreateTransactionDTO(
                    transfer.getToAccount(), TransactionTypeEnum.CREDIT,
                    transfer.getAmount(), transfer.getDescription()
            ));

            rabbitTemplate.convertAndSend(RabbitMQConfig.TRANSFER_RESPONSE_QUEUE, transferId);
            log.info("Transfer processed: id={}", transferId);
        } catch (Exception e) {
            log.error("Failed to process transfer id={}: {}", transferId, e.getMessage(), e);
        }
    }
}
