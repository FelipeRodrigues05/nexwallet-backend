package com.spring.bank.investment.config;

import com.spring.bank.investment.service.InvestmentProductCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "nexwallet.runtime.role", havingValue = "api", matchIfMissing = true)
@RequiredArgsConstructor
public class InvestmentCatalogSeeder implements ApplicationRunner {

    private final InvestmentProductCatalogService investmentProductCatalogService;

    @Override
    public void run(ApplicationArguments args) {
        investmentProductCatalogService.seedDefaults();
    }
}
