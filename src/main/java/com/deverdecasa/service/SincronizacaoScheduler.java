package com.deverdecasa.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Dispara a sincronização diária. A expressão cron vem da configuração, para que mudar o horário
 * não exija recompilar nem reempacotar a aplicação.
 */
@Component
public class SincronizacaoScheduler {

    private static final Logger log = LoggerFactory.getLogger(SincronizacaoScheduler.class);

    private final SincronizacaoService sincronizacao;

    public SincronizacaoScheduler(SincronizacaoService sincronizacao) {
        this.sincronizacao = sincronizacao;
    }

    @Scheduled(cron = "${deverdecasa.sincronizacao.cron}", zone = "America/Sao_Paulo")
    public void sincronizacaoDiaria() {
        log.info("Disparando sincronização agendada");
        sincronizacao.sincronizarTudo();
    }
}
