package com.deverdecasa.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Permite rodar a sincronização sem esperar o agendamento, que é o caminho da carga inicial:
 * {@code mvn spring-boot:run -Dspring-boot.run.arguments=--sincronizar} ou a propriedade
 * {@code deverdecasa.sincronizacao.ao-iniciar=true}.
 *
 * <p>Fica como argumento de execução em vez de endpoint HTTP de propósito: disparar uma varredura
 * das APIs públicas é operação de manutenção, não algo que deva ficar exposto na web sem controle
 * de acesso.
 */
@Component
public class SincronizacaoRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SincronizacaoRunner.class);

    private final SincronizacaoService sincronizacao;
    private final SincronizacaoProperties properties;

    public SincronizacaoRunner(SincronizacaoService sincronizacao, SincronizacaoProperties properties) {
        this.sincronizacao = sincronizacao;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.aoIniciar() && !args.containsOption("sincronizar")) {
            return;
        }
        log.info("Sincronização solicitada na inicialização");
        sincronizacao.sincronizarTudo();
    }
}
