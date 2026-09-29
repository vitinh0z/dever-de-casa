package com.deverdecasa.dto;

import com.deverdecasa.domain.Casa;
import com.deverdecasa.domain.TipoVoto;
import java.time.LocalDate;
import java.util.List;

/** O que a camada web enxerga. Entidade JPA não cruza essa fronteira. */
public final class ParlamentarDtos {

    private ParlamentarDtos() {
    }

    /** Linha da listagem de busca. */
    public record ParlamentarResumoDto(Long id,
                                       String nome,
                                       Casa casa,
                                       String siglaUf,
                                       String siglaPartido,
                                       String urlFoto,
                                       long qtdProposicoes) {

        public String tratamento() {
            return casa == Casa.SENADO ? "Senador(a)" : "Deputado(a)";
        }
    }

    public record ProposicaoDto(Long id,
                                String identificacao,
                                String ementa,
                                LocalDate dataApresentacao,
                                String situacao,
                                Boolean aprovada,
                                String urlInteiroTeor,
                                boolean proponente) {
    }

    public record VotoDto(String identificacaoProposicao,
                          String descricaoVotacao,
                          LocalDate data,
                          String siglaOrgao,
                          TipoVoto tipoVoto,
                          String descricaoOrigem,
                          boolean secreta) {
    }

    /** Votação que decidiu uma proposta do parlamentar sem apurar voto individual. */
    public record VotacaoSimbolicaDto(String identificacaoProposicao,
                                      String descricaoVotacao,
                                      LocalDate data,
                                      Boolean aprovada) {
    }

    /** Perfil completo: quem é, o que apresentou e como votou. */
    public record PerfilDto(Long id,
                            String nome,
                            String nomeCivil,
                            Casa casa,
                            String siglaUf,
                            String siglaPartido,
                            String urlFoto,
                            String email,
                            String situacao,
                            List<ProposicaoDto> proposicoes,
                            List<VotoDto> votos,
                            List<VotacaoSimbolicaDto> votacoesSimbolicas) {

        public String tratamento() {
            return casa == Casa.SENADO ? "Senador(a)" : "Deputado(a)";
        }

        public long qtdProposicoesAprovadas() {
            return proposicoes.stream().filter(p -> Boolean.TRUE.equals(p.aprovada())).count();
        }
    }
}
