package com.deverdecasa.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.deverdecasa.domain.Casa;
import com.deverdecasa.domain.Proposicao;
import com.deverdecasa.integracao.camara.CamaraDtos.ProposicaoDetalheApiResponse;
import com.deverdecasa.integracao.camara.CamaraDtos.ProposicaoDetalheApiResponse.StatusProposicao;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mapstruct.factory.Mappers;

/**
 * A marca de aprovação é o que sustenta o filtro da listagem, e ela sai de um texto livre
 * publicado pela casa. Os valores abaixo foram colhidos da própria API da Câmara.
 */
class SituacaoProposicaoTest {

    private final ProposicaoMapper mapper = Mappers.getMapper(ProposicaoMapper.class);

    @ParameterizedTest
    @ValueSource(strings = {
            "Transformado em Norma Jurídica",
            "Aguardando Apreciação pelo Senado Federal",
            "Aguardando Sanção",
            "Aguardando Autógrafos na Mesa"})
    void reconheceSituacoesDeAprovacao(String situacao) {
        assertThat(aprovadaPara(situacao)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Arquivada",
            "Retirado pelo(a) Autor(a)",
            "Aguardando Parecer",
            "Pronta para Pauta",
            "Tramitando em Conjunto",
            "Aguardando Designação de Relator(a)"})
    void naoConfundeTramitacaoComAprovacao(String situacao) {
        assertThat(aprovadaPara(situacao)).isFalse();
    }

    @Test
    void situacaoAusenteFicaDesconhecidaEmVezDeReprovada() {
        assertThat(aprovadaPara(null)).isNull();
        assertThat(aprovadaPara("  ")).isNull();
    }

    private Boolean aprovadaPara(String situacao) {
        Proposicao proposicao = new Proposicao(Casa.CAMARA, "1", "PL");
        mapper.aplicarDetalheDaCamara(proposicao, new ProposicaoDetalheApiResponse(
                1L, "PL", 1, 2026, "ementa", "2026-01-01", null,
                new StatusProposicao(situacao, "tramitação")));
        return proposicao.getAprovada();
    }
}
