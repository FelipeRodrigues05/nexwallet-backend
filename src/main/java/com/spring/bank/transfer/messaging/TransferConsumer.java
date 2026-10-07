package com.spring.bank.transfer.messaging;

import com.spring.bank.common.config.messaging.RabbitMQConfig;
import com.spring.bank.common.exception.TransferNotFoundException;
import com.spring.bank.transaction.dto.CreateTransactionDTO;
import com.spring.bank.transaction.enums.TransactionTypeEnum;
import com.spring.bank.transaction.service.CreateTransactionService;
import com.spring.bank.transfer.model.Transfer;
import com.spring.bank.transfer.repository.TransferRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "nexwallet.runtime.role", havingValue = "transfer-worker")
@RequiredArgsConstructor
public class TransferConsumer {

    private static final Logger log = LoggerFactory.getLogger(TransferConsumer.class);

    private final TransferRepository transferRepository;
    private final CreateTransactionService createTransactionService;
    private final TransferPublisher transferPublisher;

    @RabbitListener(queues = RabbitMQConfig.TRANSFER_QUEUE)
    @Transactional
    public void consume(Long transferId) {
        Transfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new TransferNotFoundException(
                        String.format("Transfer with ID %s not found", transferId)));

        try {
            createTransactionService.create(new CreateTransactionDTO(
                    transfer.getFromAccount(), TransactionTypeEnum.DEBIT,
                    transfer.getAmount(), transfer.getDescription()
            ));
            createTransactionService.create(new CreateTransactionDTO(
                    transfer.getToAccount(), TransactionTypeEnum.CREDIT,
                    transfer.getAmount(), transfer.getDescription()
            ));

            transferPublisher.publishResponse(transferId);
            log.info("Transfer processed: id={}", transferId);
        } catch (Exception error) {
            log.error("Failed to process transfer id={}: {}", transferId, error.getMessage(), error);
            throw error;
        }
    }
}
