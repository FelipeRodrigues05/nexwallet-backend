package com.spring.bank.auth.config;

import com.spring.bank.auth.dto.CreateClientDTO;
import com.spring.bank.auth.model.Client;
import com.spring.bank.auth.repository.ClientRepository;
import com.spring.bank.auth.service.CreateClientService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@ConditionalOnProperty(name = "nexwallet.runtime.role", havingValue = "api", matchIfMissing = true)
@RequiredArgsConstructor
public class ClientSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ClientSeeder.class);

    private final CreateClientService createClientService;
    private final ClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.client.default.id:nexwallet-app}")
    private String defaultClientId;

    @Value("${app.client.default.name:NexWallet App}")
    private String defaultClientName;

    @Value("${app.client.default.secret:}")
    private String defaultClientSecret;

    @Override
    public void run(ApplicationArguments args) {
        boolean hasFixedSecret = !defaultClientSecret.isBlank();
        Optional<Client> existing = clientRepository.findByClientId(defaultClientId);

        if (existing.isEmpty()) {
            var created = createClientService.create(
                    new CreateClientDTO(defaultClientId, defaultClientName),
                    hasFixedSecret ? defaultClientSecret : null
            );
            log.info("Default client seeded: clientId={} secret={}", created.clientId(), created.clientSecret());
        } else if (hasFixedSecret) {
            Client client = existing.get();
            client.setClientSecret(passwordEncoder.encode(defaultClientSecret));
            clientRepository.save(client);
            log.info("Default client secret updated: clientId={}", defaultClientId);
        }
    }
}
