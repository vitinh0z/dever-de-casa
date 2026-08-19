package com.deverdecasa.integracao.camara;

import com.deverdecasa.integracao.camara.CamaraDtos.AutorApiResponse;
import com.deverdecasa.integracao.camara.CamaraDtos.DeputadoApiResponse;
import com.deverdecasa.integracao.camara.CamaraDtos.DeputadoDetalheApiResponse;
import com.deverdecasa.integracao.camara.CamaraDtos.Envelope;
import com.deverdecasa.integracao.camara.CamaraDtos.EnvelopeUnico;
import com.deverdecasa.integracao.camara.CamaraDtos.PartidoApiResponse;
import com.deverdecasa.integracao.camara.CamaraDtos.ProposicaoApiResponse;
import com.deverdecasa.integracao.camara.CamaraDtos.ProposicaoDetalheApiResponse;
import com.deverdecasa.integracao.camara.CamaraDtos.VotacaoApiResponse;
import com.deverdecasa.integracao.camara.CamaraDtos.VotoApiResponse;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Leitura da API de Dados Abertos da Câmara dos Deputados.
 *
 * <p>Só busca e devolve resposta bruta: quem traduz para o domínio são os mappers. A API é
 * paginada por {@code pagina}/{@code itens} e sinaliza continuação com um link {@code next},
 * que é o critério usado aqui para parar — contar páginas daria resultado errado quando o total
 * muda entre duas chamadas.
 */
@Component
public class CamaraApiClient {

    private static final Logger log = LoggerFactory.getLogger(CamaraApiClient.class);
    private static final int ITENS_POR_PAGINA = 100;

    private final RestClient restClient;
    private final int maxPaginas;

    public CamaraApiClient(RestClient camaraRestClient, CamaraApiProperties properties) {
        this.restClient = camaraRestClient;
        this.maxPaginas = properties.maxPaginas();
    }

    /** Deputados em exercício na legislatura corrente. */
    public List<DeputadoApiResponse> listarDeputados() {
        return paginar("/deputados", "&ordem=ASC&ordenarPor=nome",
                new ParameterizedTypeReference<Envelope<DeputadoApiResponse>>() {
                });
    }

    public Optional<DeputadoDetalheApiResponse> buscarDeputado(String idDeputado) {
        return buscarUnico("/deputados/" + idDeputado,
                new ParameterizedTypeReference<EnvelopeUnico<DeputadoDetalheApiResponse>>() {
                });
    }

    public List<PartidoApiResponse> listarPartidos() {
        return paginar("/partidos", "&ordem=ASC&ordenarPor=sigla",
                new ParameterizedTypeReference<Envelope<PartidoApiResponse>>() {
                });
    }

    /** Proposições que o deputado assinou como autor. */
    public List<ProposicaoApiResponse> listarProposicoesPorAutor(String idDeputado) {
        return paginar("/proposicoes", "&idDeputadoAutor=" + idDeputado + "&ordem=DESC&ordenarPor=id",
                new ParameterizedTypeReference<Envelope<ProposicaoApiResponse>>() {
                });
    }

    public Optional<ProposicaoDetalheApiResponse> buscarProposicao(String idProposicao) {
        return buscarUnico("/proposicoes/" + idProposicao,
                new ParameterizedTypeReference<EnvelopeUnico<ProposicaoDetalheApiResponse>>() {
                });
    }

    public List<AutorApiResponse> listarAutoresDaProposicao(String idProposicao) {
        return paginar("/proposicoes/" + idProposicao + "/autores", "",
                new ParameterizedTypeReference<Envelope<AutorApiResponse>>() {
                });
    }

    /** Votações registradas no intervalo, de qualquer órgão da casa. */
    public List<VotacaoApiResponse> listarVotacoes(LocalDate inicio, LocalDate fim) {
        String filtros = "&dataInicio=" + inicio + "&dataFim=" + fim + "&ordem=DESC&ordenarPor=dataHoraRegistro";
        return paginar("/votacoes", filtros,
                new ParameterizedTypeReference<Envelope<VotacaoApiResponse>>() {
                });
    }

    /**
     * Votos individuais de uma votação. Lista vazia quer dizer votação simbólica: a casa
     * deliberou sem apurar voto a voto, e não que os dados estejam faltando.
     */
    public List<VotoApiResponse> listarVotos(String idVotacao) {
        return paginar("/votacoes/" + idVotacao + "/votos", "",
                new ParameterizedTypeReference<Envelope<VotoApiResponse>>() {
                });
    }

    private <T> List<T> paginar(String caminho, String filtros, ParameterizedTypeReference<Envelope<T>> tipo) {
        List<T> acumulado = new ArrayList<>();
        for (int pagina = 1; pagina <= maxPaginas; pagina++) {
            String uri = caminho + "?pagina=" + pagina + "&itens=" + ITENS_POR_PAGINA + filtros;
            Envelope<T> envelope;
            try {
                envelope = restClient.get().uri(uri).retrieve().body(tipo);
            } catch (RestClientException e) {
                log.warn("Falha ao consultar {} da Câmara: {}", uri, e.getMessage());
                break;
            }
            if (envelope == null) {
                break;
            }
            acumulado.addAll(envelope.dadosOuVazio());
            if (!envelope.temProximaPagina()) {
                return acumulado;
            }
            if (pagina == maxPaginas) {
                log.info("Limite de {} páginas atingido em {}; o restante entra na próxima sincronização.",
                        maxPaginas, caminho);
            }
        }
        return acumulado;
    }

    private <T> Optional<T> buscarUnico(String caminho, ParameterizedTypeReference<EnvelopeUnico<T>> tipo) {
        try {
            EnvelopeUnico<T> envelope = restClient.get().uri(caminho).retrieve().body(tipo);
            return Optional.ofNullable(envelope).map(EnvelopeUnico::dados);
        } catch (RestClientException e) {
            log.warn("Falha ao consultar {} da Câmara: {}", caminho, e.getMessage());
            return Optional.empty();
        }
    }
}
