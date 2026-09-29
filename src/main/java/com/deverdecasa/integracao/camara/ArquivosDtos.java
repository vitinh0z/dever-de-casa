package com.deverdecasa.integracao.camara;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Registros dos arquivos de dados abertos publicados em bloco.
 *
 * <p>São formatos distintos dos da API de consulta, ainda que descrevam as mesmas coisas: aqui a
 * proposição costuma vir identificada por URI, o voto traz o deputado embutido em
 * {@code deputado_} e a votação já chega com o placar somado.
 */
public final class ArquivosDtos {

    private ArquivosDtos() {
    }

    /** Extrai o identificador numérico do fim de uma URI de recurso da Câmara. */
    static String idDaUri(String uri) {
        if (uri == null || uri.isBlank()) {
            return null;
        }
        int barra = uri.lastIndexOf('/');
        String id = barra >= 0 ? uri.substring(barra + 1) : uri;
        return id.isBlank() ? null : id;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ProposicaoEmMassa(Long id,
                                    String siglaTipo,
                                    Integer numero,
                                    Integer ano,
                                    String ementa,
                                    String ementaDetalhada,
                                    String keywords,
                                    String dataApresentacao,
                                    String urlInteiroTeor,
                                    UltimoStatus ultimoStatus) {

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record UltimoStatus(String data,
                                   String siglaOrgao,
                                   String descricaoTramitacao,
                                   String descricaoSituacao,
                                   String despacho) {
        }

        public String situacao() {
            return ultimoStatus == null ? null : ultimoStatus.descricaoSituacao();
        }

        public String tramitacao() {
            return ultimoStatus == null ? null : ultimoStatus.descricaoTramitacao();
        }

        public String dataUltimoStatus() {
            return ultimoStatus == null ? null : ultimoStatus.data();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AutoriaEmMassa(Long idProposicao,
                                 Long idDeputadoAutor,
                                 String tipoAutor,
                                 String nomeAutor,
                                 String ordemAssinatura,
                                 String proponente) {

        /** Só assinaturas de parlamentar interessam; comissões e o Executivo também autoram. */
        public boolean deDeputado() {
            return idDeputadoAutor != null;
        }

        /** A casa marca o autor principal com 1; os demais assinam como coautores. */
        public boolean ehProponente() {
            return "1".equals(proponente);
        }

        public Integer ordem() {
            try {
                return ordemAssinatura == null || ordemAssinatura.isBlank()
                        ? null
                        : Integer.valueOf(ordemAssinatura.trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TemaEmMassa(String uriProposicao, Integer codTema, String tema, Integer relevancia) {

        public String idProposicao() {
            return idDaUri(uriProposicao);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record VotacaoEmMassa(String id,
                                 String data,
                                 String siglaOrgao,
                                 String descricao,
                                 Integer aprovacao,
                                 Integer votosSim,
                                 Integer votosNao,
                                 Integer votosOutros) {
    }

    /** Liga a votação à proposta decidida, e já traz a ementa dela. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record VotacaoProposicaoEmMassa(String idVotacao,
                                           @JsonProperty("proposicao_") ProposicaoDaVotacao proposicao) {

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record ProposicaoDaVotacao(Long id,
                                          String titulo,
                                          String ementa,
                                          String siglaTipo,
                                          Integer numero,
                                          Integer ano) {
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record VotoEmMassa(String idVotacao,
                              String dataHoraVoto,
                              String voto,
                              @JsonProperty("deputado_") DeputadoDoVoto deputado) {

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record DeputadoDoVoto(String id, String nome, String siglaPartido, String siglaUf) {
        }
    }
}
