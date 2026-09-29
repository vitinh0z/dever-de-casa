package com.deverdecasa.integracao.senado;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Objects;

/**
 * Respostas brutas da API de Dados Abertos do Senado Federal.
 *
 * <p>O Senado publica um modelo próprio, herdado de XML: envelopes aninhados com nomes em
 * PascalCase para a lista de senadores, e JSON plano nos serviços mais novos de processo e
 * votação. Os dois formatos ficam isolados aqui, para que o domínio não conheça nenhum deles.
 *
 * <p>Os nomes em PascalCase são declarados um a um com {@code @JsonProperty}, em vez de uma
 * estratégia de nomenclatura no tipo: a estratégia é instanciada pelo Spring como bean na hora de
 * desserializar e, quando isso falha, o erro aparece só em execução, como uma lista de senadores
 * vazia — o mapeamento explícito não tem esse ponto de falha.
 */
public final class SenadoDtos {

    private SenadoDtos() {
    }

    // --- lista de senadores em exercício (envelope no formato antigo) -------------------------

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ListaSenadoresResponse(
            @JsonProperty("ListaParlamentarEmExercicio") ListaParlamentarEmExercicio lista) {

        public List<Parlamentar> senadores() {
            if (lista == null || lista.parlamentares() == null) {
                return List.of();
            }
            List<Parlamentar> parlamentares = lista.parlamentares().parlamentar();
            return parlamentares == null ? List.of() : parlamentares;
        }

        /** Identificações já desembrulhadas dos dois níveis de envelope. */
        public List<IdentificacaoParlamentar> identificacoes() {
            return senadores().stream()
                    .map(Parlamentar::identificacao)
                    .filter(Objects::nonNull)
                    .toList();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ListaParlamentarEmExercicio(
            @JsonProperty("Parlamentares") Parlamentares parlamentares) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Parlamentares(@JsonProperty("Parlamentar") List<Parlamentar> parlamentar) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Parlamentar(
            @JsonProperty("IdentificacaoParlamentar") IdentificacaoParlamentar identificacao) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record IdentificacaoParlamentar(
            @JsonProperty("CodigoParlamentar") String codigoParlamentar,
            @JsonProperty("NomeParlamentar") String nomeParlamentar,
            @JsonProperty("NomeCompletoParlamentar") String nomeCompleto,
            @JsonProperty("SiglaPartidoParlamentar") String siglaPartido,
            @JsonProperty("UfParlamentar") String uf,
            @JsonProperty("UrlFotoParlamentar") String urlFoto,
            @JsonProperty("EmailParlamentar") String email) {
    }

    // --- processo legislativo (serviço que substituiu o de autorias) --------------------------

    /**
     * Matéria assinada por um senador.
     *
     * <p>{@code siglaTipoDeliberacao} é o campo que diz se a proposta foi adiante — chega como
     * {@code APROVADA_NO_PLENARIO} e afins —, o que evita interpretar o texto livre de
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
                                  @JsonProperty("siglaUFParlamentar") String siglaUf,
                                  String siglaVotoParlamentar) {
    }
}
