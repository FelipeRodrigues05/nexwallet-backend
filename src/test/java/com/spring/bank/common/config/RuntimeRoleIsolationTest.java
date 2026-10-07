package com.spring.bank.common.config;

import com.spring.bank.account.service.AccountFundsService;
import com.spring.bank.transaction.messaging.TransactionConsumer;
import com.spring.bank.transaction.messaging.TransactionPublisher;
import com.spring.bank.transaction.messaging.TransactionResponseConsumer;
import com.spring.bank.transaction.repository.TransactionRepository;
import com.spring.bank.transaction.service.CompleteTransactionService;
import com.spring.bank.transaction.service.CreateTransactionService;
import com.spring.bank.transfer.messaging.TransferConsumer;
import com.spring.bank.transfer.messaging.TransferPublisher;
import com.spring.bank.transfer.messaging.TransferResponseConsumer;
import com.spring.bank.transfer.repository.TransferRepository;
import com.spring.bank.transfer.service.CompleteTransferService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

class RuntimeRoleIsolationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(WorkerConsumersConfiguration.class)
            .withBean(TransactionRepository.class, () -> interfaceStub(TransactionRepository.class))
            .withBean(AccountFundsService.class, () -> new AccountFundsService(null, null))
            .withBean(TransactionPublisher.class, () -> new TransactionPublisher(null))
            .withBean(CompleteTransactionService.class, () -> new CompleteTransactionService(null))
            .withBean(TransferRepository.class, () -> interfaceStub(TransferRepository.class))
            .withBean(CreateTransactionService.class, () -> new CreateTransactionService(null, null, null))
            .withBean(TransferPublisher.class, () -> new TransferPublisher(null))
            .withBean(CompleteTransferService.class, () -> new CompleteTransferService(null));

    @Test
    void apiRoleStartsOnlyResponseConsumers() {
        contextRunner.withPropertyValues("nexwallet.runtime.role=api").run(context -> {
            context.getBean(TransactionResponseConsumer.class);
            context.getBean(TransferResponseConsumer.class);
            org.assertj.core.api.Assertions.assertThat(context.getBeansOfType(TransactionConsumer.class))
                    .isEmpty();
            org.assertj.core.api.Assertions.assertThat(context.getBeansOfType(TransferConsumer.class)).isEmpty();
        });
    }

    @Test
    void transactionWorkerStartsOnlyTransactionConsumers() {
        contextRunner.withPropertyValues("nexwallet.runtime.role=transaction-worker").run(context -> {
            context.getBean(TransactionConsumer.class);
            org.assertj.core.api.Assertions.assertThat(context.getBeansOfType(TransactionResponseConsumer.class))
                    .isEmpty();
            org.assertj.core.api.Assertions.assertThat(context.getBeansOfType(TransferConsumer.class)).isEmpty();
            org.assertj.core.api.Assertions.assertThat(context.getBeansOfType(TransferResponseConsumer.class))
                    .isEmpty();
        });
    }

    @Test
    void transferWorkerStartsOnlyTransferConsumers() {
        contextRunner.withPropertyValues("nexwallet.runtime.role=transfer-worker").run(context -> {
            context.getBean(TransferConsumer.class);
            org.assertj.core.api.Assertions.assertThat(context.getBeansOfType(TransferResponseConsumer.class))
                    .isEmpty();
            org.assertj.core.api.Assertions.assertThat(context.getBeansOfType(TransactionConsumer.class)).isEmpty();
            org.assertj.core.api.Assertions.assertThat(context.getBeansOfType(TransactionResponseConsumer.class))
                    .isEmpty();
        });
    }

    private static <T> T interfaceStub(Class<T> interfaceType) {
        Object stub = java.lang.reflect.Proxy.newProxyInstance(
                interfaceType.getClassLoader(),
                new Class<?>[] {interfaceType},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == arguments[0];
                    case "toString" -> interfaceType.getSimpleName() + "Stub";
                    default -> null;
                });
        return interfaceType.cast(stub);
    }

    @Configuration(proxyBeanMethods = false)
    @Import({
        TransactionConsumer.class,
        TransactionResponseConsumer.class,
        TransferConsumer.class,
        TransferResponseConsumer.class
    })
    static class WorkerConsumersConfiguration {
    }
}
