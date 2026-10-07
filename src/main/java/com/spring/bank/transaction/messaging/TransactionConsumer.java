package com.spring.bank.transaction.messaging;

import com.spring.bank.account.service.AccountFundsService;
import com.spring.bank.common.config.messaging.RabbitMQConfig;
import com.spring.bank.common.exception.TransactionNotFoundException;
import com.spring.bank.transaction.model.Transaction;
import com.spring.bank.transaction.repository.TransactionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "nexwallet.runtime.role", havingValue = "transaction-worker")
@RequiredArgsConstructor
public class TransactionConsumer {

    private static final Logger log = LoggerFactory.getLogger(TransactionConsumer.class);

    private final TransactionRepository transactionRepository;
    private final AccountFundsService accountFundsService;
    private final TransactionPublisher transactionPublisher;

    @RabbitListener(queues = RabbitMQConfig.TRANSACTION_QUEUE)
    @Transactional
    public void consume(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException(
                        String.format("Transaction with ID %s not found", transactionId)));

        Long accountId = transaction.getAccount().getId();

        try {
            switch (transaction.getType()) {
                case CREDIT, DEPOSIT -> accountFundsService.addFunds(accountId, transaction.getAmount());
                case DEBIT, WITHDRAW, FEE -> accountFundsService.subtractFunds(accountId, transaction.getAmount());
            }
            transactionPublisher.publishResponse(transactionId);
            log.debug("Transaction processed: id={} type={}", transactionId, transaction.getType());
        } catch (Exception error) {
            log.error("Failed to process transaction id={}: {}", transactionId, error.getMessage(), error);
            throw error;
        }
    }
}
