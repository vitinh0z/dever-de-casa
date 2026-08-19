package com.deverdecasa.integracao.camara;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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
 * <p>O download é gravado num arquivo temporário antes de ser interpretado, em duas etapas
 * separadas. Interpretar durante o download parece mais econômico, mas deixa a conexão parada
 * enquanto cada lote vai para o banco, e o servidor a encerra no meio do caminho — o sintoma é um
 * JSON que termina antes da hora, depois de já ter gravado parte dos registros. Com as etapas
 * separadas, a rede é usada na velocidade dela e a memória continua constante, porque a leitura
 * do arquivo em disco também é incremental.
 */
@Component
public class ArquivosCamaraClient {

    private static final Logger log = LoggerFactory.getLogger(ArquivosCamaraClient.class);
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final RestClient restClient;

    public ArquivosCamaraClient(RestClient arquivosCamaraRestClient) {
        this.restClient = arquivosCamaraRestClient;
    }

    public <T> long lerAno(ArquivoEmMassa arquivo, int ano, Class<T> tipo, Consumer<T> destino) {
        return ler(arquivo.caminho(ano), tipo, destino);
    }

    public <T> long ler(String caminho, Class<T> tipo, Consumer<T> destino) {
        long inicio = System.currentTimeMillis();
        Path temporario = null;
        try {
            temporario = baixar(caminho);
            if (temporario == null) {
                return 0;
            }
            long lidos = interpretar(temporario, tipo, destino);
            log.info("Arquivo {} lido: {} registros em {}s", caminho, lidos,
                    (System.currentTimeMillis() - inicio) / 1000);
            return lidos;
        } catch (IOException e) {
            log.warn("Falha ao ler o arquivo {}: {}", caminho, e.toString());
            return 0;
        } finally {
            apagar(temporario);
        }
    }

    private Path baixar(String caminho) throws IOException {
        log.info("Baixando arquivo de dados abertos: {}", caminho);
        Path destino = Files.createTempFile("dever-de-casa-", ".json");
        Boolean baixado = restClient.get()
                .uri(caminho)
                .exchange((requisicao, resposta) -> {
                    if (!resposta.getStatusCode().is2xxSuccessful()) {
                        log.warn("Arquivo {} indisponível: HTTP {}", caminho, resposta.getStatusCode());
                        return false;
                    }
                    try (InputStream corpo = resposta.getBody()) {
                        Files.copy(corpo, destino, StandardCopyOption.REPLACE_EXISTING);
                    }
                    return true;
                });

        if (!Boolean.TRUE.equals(baixado)) {
            apagar(destino);
            return null;
        }
        log.info("Arquivo {} baixado: {} MB", caminho, Files.size(destino) / (1024 * 1024));
        return destino;
    }

    /**
     * Percorre o JSON registro a registro.
     *
     * <p>Alguns arquivos são um array na raiz e outros vêm embrulhados em {@code {"dados": [...]}}.
     * Em vez de decidir pelo nome do arquivo, o parser avança até o primeiro array que encontrar,
     * o que atende aos dois formatos e não quebra se a casa mudar o embrulho.
     */
    private <T> long interpretar(Path arquivo, Class<T> tipo, Consumer<T> destino) throws IOException {
        long total = 0;
        try (InputStream entrada = Files.newInputStream(arquivo);
             JsonParser parser = MAPPER.getFactory().createParser(entrada)) {
            JsonToken token = parser.nextToken();
            while (token != null && token != JsonToken.START_ARRAY) {
                token = parser.nextToken();
            }
            if (token == null) {
                return 0;
            }
            while (parser.nextToken() == JsonToken.START_OBJECT) {
                destino.accept(MAPPER.readValue(parser, tipo));
                total++;
            }
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
        return total;
    }

    private void apagar(Path arquivo) {
        if (arquivo == null) {
            return;
        }
        try {
            Files.deleteIfExists(arquivo);
        } catch (IOException e) {
            log.warn("Não foi possível remover o arquivo temporário {}: {}", arquivo, e.toString());
        }
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
}
