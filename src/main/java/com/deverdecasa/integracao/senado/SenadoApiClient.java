package com.deverdecasa.integracao.senado;

import com.deverdecasa.integracao.senado.SenadoDtos.IdentificacaoParlamentar;
import com.deverdecasa.integracao.senado.SenadoDtos.ListaSenadoresResponse;
import com.deverdecasa.integracao.senado.SenadoDtos.ProcessoApiResponse;
import com.deverdecasa.integracao.senado.SenadoDtos.VotacaoApiResponse;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Leitura da API de Dados Abertos do Senado Federal.
 *
 * <p>Não compartilha nada com o cliente da Câmara de propósito: as duas casas expõem modelos
 * diferentes — aqui a lista de senadores vem num envelope herdado de XML, as matérias saem do
 * serviço de processo legislativo e cada votação já traz os votos embutidos, em vez de deixá-los
 * num recurso à parte. Tentar unificar os dois clientes só espalharia condicionais.
 *
 * <p>Os serviços {@code /senador/{id}/autorias} e {@code /senador/{id}/votacoes} não são usados:
 * ambos estão marcados como descontinuados pela própria casa, que aponta {@code /processo} e
 * {@code /votacao} como substitutos.
 */
@Component
public class SenadoApiClient {

    private static final Logger log = LoggerFactory.getLogger(SenadoApiClient.class);

    private final RestClient restClient;
    private final int anosDeHistorico;

    public SenadoApiClient(RestClient senadoRestClient, SenadoApiProperties properties) {
        this.restClient = senadoRestClient;
        this.anosDeHistorico = properties.anosDeHistorico();
    }

    /** Senadores em exercício. */
    public List<IdentificacaoParlamentar> listarSenadores() {
        try {
            ListaSenadoresResponse resposta = restClient.get()
                    .uri("/senador/lista/atual.json")
                    .retrieve()
                    .body(ListaSenadoresResponse.class);
            if (resposta == null) {
                return List.of();
            }
            return resposta.senadores().stream()
                    .map(SenadoDtos.Parlamentar::identificacaoParlamentar)
                    .filter(java.util.Objects::nonNull)
                    .toList();
        } catch (RestClientException e) {
            log.warn("Falha ao listar senadores: {}", e.getMessage());
            return List.of();
        }
    }

    /** Matérias assinadas pelo senador, ano a ano dentro da janela configurada. */
    public List<ProcessoApiResponse> listarMateriasPorAutor(String codigoParlamentar) {
        List<ProcessoApiResponse> acumulado = new ArrayList<>();
        for (int ano : anos()) {
            String uri = "/processo?codigoParlamentarAutor=" + codigoParlamentar + "&ano=" + ano;
            acumulado.addAll(buscarLista(uri, new ParameterizedTypeReference<List<ProcessoApiResponse>>() {
            }));
        }
        return acumulado;
    }

    /** Votações do período, cada uma já com os votos de todos os senadores. */
    public List<VotacaoApiResponse> listarVotacoes() {
        List<VotacaoApiResponse> acumulado = new ArrayList<>();
        for (int ano : anos()) {
            String uri = "/votacao?ano=" + ano;
            acumulado.addAll(buscarLista(uri, new ParameterizedTypeReference<List<VotacaoApiResponse>>() {
            }));
        }
        return acumulado;
    }

    private <T> List<T> buscarLista(String uri, ParameterizedTypeReference<List<T>> tipo) {
        try {
            List<T> corpo = restClient.get().uri(uri).retrieve().body(tipo);
            return corpo == null ? List.of() : corpo;
        } catch (RestClientException e) {
            log.warn("Falha ao consultar {} do Senado: {}", uri, e.getMessage());
            return List.of();
        }
    }

    private List<Integer> anos() {
        int atual = Year.now().getValue();
        List<Integer> anos = new ArrayList<>();
        for (int i = 0; i < anosDeHistorico; i++) {
            anos.add(atual - i);
        }
        return anos;
    }
}
