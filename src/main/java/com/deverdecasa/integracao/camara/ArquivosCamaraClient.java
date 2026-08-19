package com.deverdecasa.integracao.camara;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Leitura dos arquivos de dados abertos que a Câmara publica em bloco, um por ano.
 *
 * <p>É o caminho da carga inicial. Montar a base pela API de consulta custaria uma requisição por
 * proposição só para descobrir a situação de tramitação — dezenas de milhares de chamadas contra
 * um serviço público — enquanto o mesmo conteúdo sai em um punhado de downloads. A sincronização
 * diária continua pela API, que é barata para o movimento de um dia.
 *
 * <p>Os arquivos são grandes: só as proposições de um ano passam de 60 MB e 40 mil registros. Por
 * isso a leitura é incremental, registro a registro, com o JSON sendo consumido enquanto ainda
 * está chegando — carregar o arquivo inteiro em memória derrubaria a aplicação com o crescimento
 * natural da base.
 */
@Component
public class ArquivosCamaraClient {

    private static final Logger log = LoggerFactory.getLogger(ArquivosCamaraClient.class);

    private final RestClient restClient;

    public ArquivosCamaraClient(RestClient arquivosCamaraRestClient) {
        this.restClient = arquivosCamaraRestClient;
    }

    /** Deputados, proposições, autores, temas, votações e votos, cada um em seu arquivo. */
    public <T> long lerAno(ArquivoEmMassa arquivo, int ano, Class<T> tipo, Consumer<T> destino) {
        return ler(arquivo.caminho(ano), tipo, destino);
    }

    public <T> long ler(String caminho, Class<T> tipo, Consumer<T> destino) {
        log.info("Lendo arquivo de dados abertos: {}", caminho);
        long inicio = System.currentTimeMillis();
        long lidos = restClient.get()
                .uri(caminho)
                .exchange((requisicao, resposta) -> {
                    if (!resposta.getStatusCode().is2xxSuccessful()) {
                        log.warn("Arquivo {} indisponível: HTTP {}", caminho, resposta.getStatusCode());
                        return 0L;
                    }
                    try (InputStream corpo = resposta.getBody()) {
                        return consumir(corpo, tipo, destino);
                    }
                });
        log.info("Arquivo {} lido: {} registros em {}s", caminho, lidos,
                (System.currentTimeMillis() - inicio) / 1000);
        return lidos;
    }

    /**
     * Percorre o JSON registro a registro.
     *
     * <p>Alguns arquivos são um array na raiz e outros vêm embrulhados em {@code {"dados": [...]}}.
     * Em vez de decidir pelo nome do arquivo, o parser avança até o primeiro array que encontrar,
     * o que funciona para os dois formatos e não quebra se a casa mudar o embrulho.
     */
    private <T> long consumir(InputStream corpo, Class<T> tipo, Consumer<T> destino) throws IOException {
        ObjectMapper mapper = MapperDeArquivos.INSTANCIA;
        long total = 0;
        try (JsonParser parser = mapper.getFactory().createParser(corpo)) {
            JsonToken token = parser.nextToken();
            while (token != null && token != JsonToken.START_ARRAY) {
                token = parser.nextToken();
            }
            if (token == null) {
                return 0;
            }
            while (parser.nextToken() == JsonToken.START_OBJECT) {
                T registro = mapper.readValue(parser, tipo);
                destino.accept(registro);
                total++;
            }
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
        return total;
    }

    /** Arquivos usados na carga, com o caminho que cada um ocupa no servidor da Câmara. */
    public enum ArquivoEmMassa {
        PROPOSICOES("proposicoes"),
        PROPOSICOES_AUTORES("proposicoesAutores"),
        PROPOSICOES_TEMAS("proposicoesTemas"),
        VOTACOES("votacoes"),
        VOTACOES_PROPOSICOES("votacoesProposicoes"),
        VOTACOES_VOTOS("votacoesVotos");

        private final String pasta;

        ArquivoEmMassa(String pasta) {
            this.pasta = pasta;
        }

        public String caminho(int ano) {
            return "/%s/json/%s-%d.json".formatted(pasta, pasta, ano);
        }
    }

    /** Mapper próprio: ignora campo novo publicado pela casa em vez de interromper a carga. */
    private static final class MapperDeArquivos {
        private static final ObjectMapper INSTANCIA = new ObjectMapper()
                .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
}
