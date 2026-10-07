package com.spring.bank.common.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Keeps scheduled domain jobs in the API process while the first worker
 * services share the application binary. Without this boundary every worker
 * instance would execute every scheduled banking job.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "nexwallet.runtime.role", havingValue = "api", matchIfMissing = true)
public class ApiSchedulingConfiguration {
}
