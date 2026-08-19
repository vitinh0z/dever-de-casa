package com.deverdecasa.mapper;

import com.deverdecasa.domain.Casa;
import com.deverdecasa.domain.Proposicao;
import com.deverdecasa.domain.ProposicaoAutor;
import com.deverdecasa.domain.TipoVoto;
import com.deverdecasa.domain.Votacao;
import com.deverdecasa.domain.VotoParlamentar;
import com.deverdecasa.dto.ParlamentarDtos.ProposicaoDto;
import com.deverdecasa.dto.ParlamentarDtos.VotacaoSimbolicaDto;
import com.deverdecasa.dto.ParlamentarDtos.VotoDto;
import com.deverdecasa.integracao.camara.CamaraDtos.ProposicaoApiResponse;
import com.deverdecasa.integracao.camara.CamaraDtos.ProposicaoDetalheApiResponse;
import com.deverdecasa.integracao.camara.CamaraDtos.VotacaoApiResponse;
import java.time.Instant;
import org.mapstruct.Mapper;

/** Traduz proposições e votações entre a resposta da casa, a entidade e o DTO de saída. */
@Mapper
public interface ProposicaoMapper {

    // --- resposta da API -> entidade ---------------------------------------------------------

    default Proposicao aplicarDaCamara(Proposicao destino, ProposicaoApiResponse origem) {
        Proposicao proposicao = destino != null
                ? destino
                : new Proposicao(Casa.CAMARA, String.valueOf(origem.id()), origem.siglaTipo());
        proposicao.setSiglaTipo(origem.siglaTipo());
        proposicao.setNumero(origem.numero());
        proposicao.setAno(origem.ano());
        proposicao.setEmenta(origem.ementa());
        proposicao.setDataApresentacao(Datas.paraData(origem.dataApresentacao()));
        proposicao.setAtualizadoEm(Instant.now());
        return proposicao;
    }

    /** A situação de tramitação só existe no detalhe, e é dela que sai a marca de aprovação. */
    default void aplicarDetalheDaCamara(Proposicao destino, ProposicaoDetalheApiResponse origem) {
        var status = origem.statusProposicao();
        String situacao = status == null ? null : status.descricaoSituacao();
        destino.setSituacao(situacao);
        destino.setAprovada(SituacaoProposicao.aprovada(situacao));
        if (origem.urlInteiroTeor() != null) {
            destino.setUrlInteiroTeor(origem.urlInteiroTeor());
        }
        if (origem.ementa() != null) {
            destino.setEmenta(origem.ementa());
        }
        destino.setAtualizadoEm(Instant.now());
    }

    /**
     * {@code aprovacao} chega como 1 ou 0; nulo significa que a casa não publicou o resultado, e
     * não que a votação foi rejeitada.
     */
    default Votacao aplicarDaCamara(Votacao destino, VotacaoApiResponse origem) {
        Votacao votacao = destino != null ? destino : new Votacao(Casa.CAMARA, origem.id());
        votacao.setData(Datas.paraData(origem.data()));
        votacao.setDescricao(origem.descricao());
        votacao.setSiglaOrgao(origem.siglaOrgao());
        votacao.setAprovada(origem.aprovacao() == null ? null : origem.aprovacao() == 1);
        return votacao;
    }

    // --- entidade -> DTO de saída ------------------------------------------------------------

    default ProposicaoDto paraDto(ProposicaoAutor autoria) {
        Proposicao p = autoria.getProposicao();
        return new ProposicaoDto(p.getId(), p.getIdentificacao(), p.getEmenta(), p.getDataApresentacao(),
                p.getSituacao(), p.getAprovada(), p.getUrlInteiroTeor(), autoria.isProponente());
    }

    default VotoDto paraDto(VotoParlamentar voto) {
        Votacao votacao = voto.getVotacao();
        Proposicao proposicao = votacao.getProposicao();
        return new VotoDto(
                proposicao == null ? null : proposicao.getIdentificacao(),
                votacao.getDescricao(),
                votacao.getData(),
                votacao.getSiglaOrgao(),
                voto.getTipoVoto(),
                voto.getDescricaoOrigem(),
                votacao.isSecreta());
    }

    default VotacaoSimbolicaDto paraSimbolicaDto(Votacao votacao) {
        Proposicao proposicao = votacao.getProposicao();
        return new VotacaoSimbolicaDto(
                proposicao == null ? null : proposicao.getIdentificacao(),
                votacao.getDescricao(),
                votacao.getData(),
                votacao.getAprovada());
    }

    /** Exposto para o serviço de sincronização traduzir o rótulo de voto de qualquer casa. */
    default TipoVoto tipoVoto(String descricao) {
        return TipoVoto.daDescricao(descricao);
    }
}
