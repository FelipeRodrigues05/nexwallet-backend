package com.spring.bank.common.config;

import com.spring.bank.domain.dto.client.ClientCreatedResponseDTO;
import com.spring.bank.domain.dto.client.CreateClientDTO;
import com.spring.bank.domain.model.Client;
import com.spring.bank.domain.repository.ClientRepository;
import com.spring.bank.domain.service.ClientService;
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
@ConditionalOnProperty(name = "nexwallet.runtime.role", havingValue = "legacy-domain")
@RequiredArgsConstructor
public class ClientSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ClientSeeder.class);

    private final ClientService clientService;
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
            ClientCreatedResponseDTO created = clientService.create(
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
