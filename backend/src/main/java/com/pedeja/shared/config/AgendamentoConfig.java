package com.pedeja.shared.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Liga os @Scheduled (hoje, o publicador do outbox). Os testes podem desligar. */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.agendamento.enabled", havingValue = "true", matchIfMissing = true)
public class AgendamentoConfig {
}
