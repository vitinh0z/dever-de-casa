package com.deverdecasa.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Limites da sincronização com as casas legislativas.
 *
 * <p>Existem porque as duas APIs são grandes e públicas: varrer tudo de uma vez castigaria o
 * serviço de quem publica o dado e demoraria demais para terminar. Cada execução avança até o
 * teto configurado e a próxima continua de onde parou.
 *
 * @param habilitada             liga a sincronização, agendada ou manual
 * @param cron                   quando o agendamento dispara
 * @param maxParlamentares       quantos parlamentares processar por execução (0 = sem limite)
 * @param maxProposicoesPorAutor teto de proposições lidas por parlamentar
 * @param diasDeVotacoes         janela de votações lida a cada execução
 * @param detalharProposicoes    buscar o detalhe de cada proposição para saber a situação; é o
 *                               passo caro, porque custa uma requisição por proposição
 * @param aoIniciar              dispara uma sincronização logo depois do boot, que é como se roda
 *                               a carga inicial sem esperar o horário do agendamento
 */
@ConfigurationProperties(prefix = "deverdecasa.sincronizacao")
public record SincronizacaoProperties(boolean habilitada,
                                      String cron,
                                      int maxParlamentares,
                                      int maxProposicoesPorAutor,
                                      int diasDeVotacoes,
                                      boolean detalharProposicoes,
                                      boolean aoIniciar) {

    public SincronizacaoProperties {
        if (cron == null || cron.isBlank()) {
            cron = "0 0 4 * * *";
        }
        if (maxProposicoesPorAutor <= 0) {
            maxProposicoesPorAutor = 50;
        }
        if (diasDeVotacoes <= 0) {
            diasDeVotacoes = 30;
        }
    }

    public boolean semLimiteDeParlamentares() {
        return maxParlamentares <= 0;
    }
}
