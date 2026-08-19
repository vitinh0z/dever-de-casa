package com.deverdecasa.integracao.camara;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Respostas brutas da API de Dados Abertos da Câmara, no formato em que ela publica.
 *
 * <p>Ficam agrupadas porque só existem para atravessar a fronteira HTTP: quem consome o domínio
 * nunca vê esses tipos. Campos não usados são ignorados de propósito, para que a inclusão de um
 * campo novo pela Câmara não quebre a sincronização.
 */
public final class CamaraDtos {

    private CamaraDtos() {
    }

    /** Envelope padrão: a API devolve sempre {@code dados} e {@code links}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Envelope<T>(List<T> dados, List<Link> links) {

        public List<T> dadosOuVazio() {
            return dados == null ? List.of() : dados;
        }

        /** Existe próxima página quando a API publica um link {@code next}. */
        public boolean temProximaPagina() {
            return links != null && links.stream().anyMatch(l -> "next".equals(l.rel()));
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EnvelopeUnico<T>(T dados) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Link(String rel, String href) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DeputadoApiResponse(Long id,
                                      String nome,
                                      String siglaPartido,
                                      String siglaUf,
                                      Integer idLegislatura,
                                      String urlFoto,
                                      String email) {
    }

    /** Detalhe de um deputado: os dados do mandato vêm aninhados em {@code ultimoStatus}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DeputadoDetalheApiResponse(Long id, String nomeCivil, UltimoStatus ultimoStatus) {

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record UltimoStatus(String nomeEleitoral,
                                   String siglaPartido,
                                   String siglaUf,
                                   Integer idLegislatura,
                                   String urlFoto,
                                   String email,
                                   String situacao,
                                   String condicaoEleitoral) {
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ProposicaoApiResponse(Long id,
                                        String siglaTipo,
                                        Integer numero,
                                        Integer ano,
                                        String ementa,
                                        String dataApresentacao) {
    }

    /** Detalhe da proposição: a situação de tramitação só existe aqui, não na listagem. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ProposicaoDetalheApiResponse(Long id,
                                               String siglaTipo,
                                               Integer numero,
                                               Integer ano,
                                               String ementa,
                                               String dataApresentacao,
                                               String urlInteiroTeor,
                                               StatusProposicao statusProposicao) {

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record StatusProposicao(String descricaoSituacao, String descricaoTramitacao) {
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AutorApiResponse(String nome, String uri, String tipo, Integer ordemAssinatura, Integer proponente) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record VotacaoApiResponse(String id,
                                     String data,
                                     String siglaOrgao,
                                     String descricao,
                                     Integer aprovacao) {
    }

    /**
     * Voto individual. O objeto do deputado chega na chave {@code deputado_}, com underscore
     * final, que é como a Câmara publica.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record VotoApiResponse(String tipoVoto,
                                  String dataRegistroVoto,
                                  @JsonProperty("deputado_") DeputadoApiResponse deputado) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PartidoApiResponse(Long id, String sigla, String nome) {
    }
}
