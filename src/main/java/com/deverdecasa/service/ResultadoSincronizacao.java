package com.deverdecasa.service;

/** O que uma execução de sincronização produziu, para registro em log e para o disparo manual. */
public record ResultadoSincronizacao(int parlamentares,
                                     int proposicoes,
                                     int votacoes,
                                     int votos,
                                     int falhas) {

    public static ResultadoSincronizacao vazio() {
        return new ResultadoSincronizacao(0, 0, 0, 0, 0);
    }

    public ResultadoSincronizacao mais(ResultadoSincronizacao outro) {
        return new ResultadoSincronizacao(
                parlamentares + outro.parlamentares,
                proposicoes + outro.proposicoes,
                votacoes + outro.votacoes,
                votos + outro.votos,
                falhas + outro.falhas);
    }

    @Override
    public String toString() {
        return "%d parlamentares, %d proposições, %d votações, %d votos, %d falhas"
                .formatted(parlamentares, proposicoes, votacoes, votos, falhas);
    }
}
