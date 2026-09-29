package com.deverdecasa.mapper;

import com.deverdecasa.domain.Casa;
import com.deverdecasa.domain.Parlamentar;
import com.deverdecasa.dto.ParlamentarDtos.ParlamentarResumoDto;
import com.deverdecasa.integracao.camara.CamaraDtos.DeputadoApiResponse;
import com.deverdecasa.integracao.camara.CamaraDtos.DeputadoDetalheApiResponse;
import com.deverdecasa.repository.ParlamentarResumo;
import java.time.Instant;
import org.mapstruct.Mapper;

/**
 * Traduz parlamentar entre as três formas em que ele aparece: resposta bruta da casa, entidade
 * e DTO de saída.
 *
 * <p>O caminho API -> entidade é escrito à mão porque não é cópia de campo: decide o nome que
 * será exibido, preserva o que já existe quando a resposta vem incompleta e deixa o partido a
 * cargo de quem tem o repositório em mãos. O caminho entidade/projeção -> DTO, esse sim mera
 * transposição, fica com o MapStruct.
 */
@Mapper
public interface ParlamentarMapper {

    ParlamentarResumoDto paraResumo(ParlamentarResumo origem);

    /** Cria ou atualiza o parlamentar a partir da listagem de deputados da Câmara. */
    default Parlamentar aplicarDaCamara(Parlamentar destino, DeputadoApiResponse origem) {
        Parlamentar parlamentar = destino != null
                ? destino
                : new Parlamentar(Casa.CAMARA, String.valueOf(origem.id()), origem.nome());
        parlamentar.setNome(origem.nome());
        parlamentar.setSiglaUf(origem.siglaUf());
        parlamentar.setUrlFoto(origem.urlFoto());
        parlamentar.setEmail(origem.email());
        parlamentar.setIdLegislatura(origem.idLegislatura());
        parlamentar.setAtualizadoEm(Instant.now());
        return parlamentar;
    }

    /**
     * Completa o parlamentar com o detalhe da Câmara, onde moram nome civil, situação e condição
     * eleitoral. O nome exibido passa a ser o nome eleitoral, que é como ele assina o mandato e
     * como o cidadão vai procurar.
     */
    default void aplicarDetalheDaCamara(Parlamentar destino, DeputadoDetalheApiResponse origem) {
        destino.setNomeCivil(origem.nomeCivil());
        var status = origem.ultimoStatus();
        if (status == null) {
            return;
        }
        if (status.nomeEleitoral() != null && !status.nomeEleitoral().isBlank()) {
            destino.setNome(status.nomeEleitoral());
        }
        destino.setSituacao(status.situacao());
        destino.setCondicaoEleitoral(status.condicaoEleitoral());
        if (status.siglaUf() != null) {
            destino.setSiglaUf(status.siglaUf());
        }
        if (status.urlFoto() != null) {
            destino.setUrlFoto(status.urlFoto());
        }
        if (status.email() != null) {
            destino.setEmail(status.email());
        }
        if (status.idLegislatura() != null) {
            destino.setIdLegislatura(status.idLegislatura());
        }
        destino.setAtualizadoEm(Instant.now());
    }
}
