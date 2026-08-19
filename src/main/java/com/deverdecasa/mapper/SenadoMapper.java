package com.deverdecasa.mapper;

import com.deverdecasa.domain.Casa;
import com.deverdecasa.domain.Parlamentar;
import com.deverdecasa.domain.Proposicao;
import com.deverdecasa.domain.Votacao;
import com.deverdecasa.integracao.senado.SenadoDtos.IdentificacaoParlamentar;
import com.deverdecasa.integracao.senado.SenadoDtos.ProcessoApiResponse;
import com.deverdecasa.integracao.senado.SenadoDtos.VotacaoApiResponse;
import java.time.Instant;
import java.util.Locale;
import org.mapstruct.Mapper;

/**
 * Traduz as respostas do Senado para as mesmas entidades que a Câmara alimenta.
 *
 * <p>É aqui que as duas casas param de ser diferentes: o resto da aplicação — busca, filtros,
 * telas — trabalha só com {@code Parlamentar}, {@code Proposicao} e {@code Votacao}, sem saber de
 * onde o registro veio, exceto pelo campo {@code casa}.
 */
@Mapper
public interface SenadoMapper {

    default Parlamentar aplicar(Parlamentar destino, IdentificacaoParlamentar origem) {
        Parlamentar parlamentar = destino != null
                ? destino
                : new Parlamentar(Casa.SENADO, origem.codigoParlamentar(), origem.nomeParlamentar());
        parlamentar.setNome(origem.nomeParlamentar());
        parlamentar.setNomeCivil(origem.nomeCompletoParlamentar());
        parlamentar.setSiglaUf(origem.ufParlamentar());
        parlamentar.setUrlFoto(origem.urlFotoParlamentar());
        parlamentar.setEmail(origem.emailParlamentar());
        parlamentar.setSituacao("Exercício");
        parlamentar.setAtualizadoEm(Instant.now());
        return parlamentar;
    }

    /**
     * {@code identificacao} chega pronta como "PL 22/2025"; a sigla e o número saem dela porque o
     * serviço de processo não publica os dois campos separados.
     */
    default Proposicao aplicar(Proposicao destino, ProcessoApiResponse origem) {
        String identificacao = origem.identificacao() == null ? "" : origem.identificacao().trim();
        String sigla = identificacao.isBlank() ? "MATÉRIA" : identificacao.split("\\s+")[0];

        Proposicao proposicao = destino != null
                ? destino
                : new Proposicao(Casa.SENADO, String.valueOf(origem.id()), sigla);
        proposicao.setSiglaTipo(sigla);
        proposicao.setNumero(numeroDe(identificacao));
        proposicao.setAno(anoDe(identificacao));
        proposicao.setEmenta(origem.ementa());
        proposicao.setDataApresentacao(Datas.paraData(origem.dataApresentacao()));
        proposicao.setSituacao(origem.situacaoAtual());
        proposicao.setAprovada(aprovada(origem));
        proposicao.setUrlInteiroTeor(origem.urlDocumento());
        proposicao.setAtualizadoEm(Instant.now());
        return proposicao;
    }

    default Votacao aplicar(Votacao destino, VotacaoApiResponse origem) {
        Votacao votacao = destino != null
                ? destino
                : new Votacao(Casa.SENADO, String.valueOf(origem.codigoSessaoVotacao()));
        votacao.setData(Datas.paraData(origem.dataSessao()));
        votacao.setDescricao(origem.descricaoVotacao());
        votacao.setSiglaOrgao("PLEN");
        votacao.setAprovada(origem.aprovada());
        votacao.setNominal(!origem.votosOuVazio().isEmpty());
        votacao.setSecreta(origem.secreta());
        return votacao;
    }

    /**
     * O Senado publica o desfecho num campo próprio, {@code siglaTipoDeliberacao}, com valores do
     * tipo {@code APROVADA_NO_PLENARIO}. É mais confiável que interpretar o texto livre da
     * situação, então é ele que decide; sem esse campo, a matéria fica como desconhecida.
     */
    private static Boolean aprovada(ProcessoApiResponse origem) {
        String deliberacao = origem.siglaTipoDeliberacao();
        if (deliberacao == null || deliberacao.isBlank()) {
            return null;
        }
        return deliberacao.toUpperCase(Locale.ROOT).startsWith("APROVAD");
    }

    private static Integer numeroDe(String identificacao) {
        return trechoNumerico(identificacao, 0);
    }

    private static Integer anoDe(String identificacao) {
        return trechoNumerico(identificacao, 1);
    }

    /** Extrai número e ano de um rótulo como "PLP 22/2025". */
    private static Integer trechoNumerico(String identificacao, int posicao) {
        if (identificacao == null || !identificacao.contains("/")) {
            return null;
        }
        String[] partes = identificacao.split("/");
        String alvo = posicao == 0 ? partes[0] : partes[partes.length - 1];
        String digitos = alvo.replaceAll("\\D", "");
        if (digitos.isEmpty()) {
            return null;
        }
        try {
            return Integer.valueOf(digitos);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
