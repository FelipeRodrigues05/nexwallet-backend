package com.spring.bank.domain.listener;

import com.spring.bank.common.config.messaging.RabbitMQConfig;
import com.spring.bank.common.exception.TransactionNotFoundException;
import com.spring.bank.domain.model.Transaction;
import com.spring.bank.domain.repository.TransactionRepository;
import com.spring.bank.domain.service.AccountService;
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
public class TransactionProcessor {

    private static final Logger log = LoggerFactory.getLogger(TransactionProcessor.class);

    private final TransactionRepository transactionRepository;
    private final AccountService accountService;
    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = RabbitMQConfig.TRANSACTION_QUEUE)
    @Transactional
    public void process(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException(
                        String.format("Transaction with ID %s not found", transactionId)));

        Long accountId = transaction.getAccount().getId();

        try {
            switch (transaction.getType()) {
                case CREDIT, DEPOSIT -> accountService.addFunds(accountId, transaction.getAmount());
                case DEBIT, WITHDRAW, FEE -> accountService.subtractFunds(accountId, transaction.getAmount());
            }
            rabbitTemplate.convertAndSend(RabbitMQConfig.TRANSACTION_RESPONSE_QUEUE, transactionId);
            log.debug("Transaction processed: id={} type={}", transactionId, transaction.getType());
        } catch (Exception e) {
            log.error("Failed to process transaction id={}: {}", transactionId, e.getMessage(), e);
        }
    }
}
