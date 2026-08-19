package com.deverdecasa.integracao.senado;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;

/**
 * Respostas brutas da API de Dados Abertos do Senado Federal.
 *
 * <p>O Senado publica um modelo próprio, herdado de XML: envelopes aninhados com nomes em
 * CamelCase para a lista de senadores, e JSON plano nos serviços mais novos de processo e
 * votação. Os dois formatos ficam isolados aqui, para que o domínio não precise conhecer nenhum
 * deles nem se pareça com um ou com outro.
 */
public final class SenadoDtos {

    private SenadoDtos() {
    }

    // --- lista de senadores em exercício (envelope no formato antigo) -------------------------

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    public record ListaSenadoresResponse(ListaParlamentarEmExercicio listaParlamentarEmExercicio) {

        public List<Parlamentar> senadores() {
            if (listaParlamentarEmExercicio == null || listaParlamentarEmExercicio.parlamentares() == null) {
                return List.of();
            }
            List<Parlamentar> lista = listaParlamentarEmExercicio.parlamentares().parlamentar();
            return lista == null ? List.of() : lista;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    public record ListaParlamentarEmExercicio(Parlamentares parlamentares) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    public record Parlamentares(List<Parlamentar> parlamentar) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    public record Parlamentar(IdentificacaoParlamentar identificacaoParlamentar) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    public record IdentificacaoParlamentar(String codigoParlamentar,
                                           String nomeParlamentar,
                                           String nomeCompletoParlamentar,
                                           String siglaPartidoParlamentar,
                                           String ufParlamentar,
                                           String urlFotoParlamentar,
                                           String emailParlamentar) {
    }

    // --- processo legislativo (serviço que substituiu o de autorias) --------------------------

    /**
     * Matéria assinada por um senador.
     *
     * <p>{@code siglaTipoDeliberacao} é o campo que diz se a proposta foi adiante — chega como
     * {@code APROVADA_NO_PLENARIO} e afins —, o que evita ter que interpretar o texto livre de
     * {@code situacaoAtual}.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ProcessoApiResponse(Long id,
                                      Long codigoMateria,
                                      String identificacao,
                                      String ementa,
                                      String dataApresentacao,
                                      String situacaoAtual,
                                      String siglaTipoDeliberacao,
                                      String tipoDocumento,
                                      String urlDocumento,
                                      String autoria) {
    }

    // --- votações (serviço que substituiu o de votações por senador) --------------------------

    /**
     * Votação do Senado, que já vem com os votos de todos os senadores embutidos, diferente da
     * Câmara, onde os votos são um recurso à parte.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record VotacaoApiResponse(Long codigoSessaoVotacao,
                                     Long codigoMateria,
                                     Long idProcesso,
                                     String identificacao,
                                     String descricaoVotacao,
                                     String ementa,
                                     String dataSessao,
                                     String resultadoVotacao,
                                     String votacaoSecreta,
                                     List<VotoApiResponse> votos) {

        /** O Senado marca o resultado com "A" de aprovada. */
        public Boolean aprovada() {
            if (resultadoVotacao == null || resultadoVotacao.isBlank()) {
                return null;
            }
            return "A".equalsIgnoreCase(resultadoVotacao.trim());
        }

        public boolean secreta() {
            return "S".equalsIgnoreCase(votacaoSecreta);
        }

        public List<VotoApiResponse> votosOuVazio() {
            return votos == null ? List.of() : votos;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record VotoApiResponse(Long codigoParlamentar,
                                  String nomeParlamentar,
                                  String siglaPartidoParlamentar,
                                  String siglaUFParlamentar,
                                  String siglaVotoParlamentar) {
    }
}
