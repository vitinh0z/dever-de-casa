package com.deverdecasa.integracao.camara;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Ajustes da integração com a Câmara.
 *
 * @param baseUrl    raiz da API de Dados Abertos
 * @param timeout    corte de paciência por requisição
 * @param maxPaginas teto de páginas por consulta, para que uma sincronização não varra a API
 *                   inteira numa única execução
 * @param arquivosUrl raiz dos arquivos publicados em bloco, usados na carga inicial
 * @param anosDeImportacao quantos anos para trás a carga em massa percorre
 */
@ConfigurationProperties(prefix = "deverdecasa.camara")
public record CamaraApiProperties(String baseUrl, Duration timeout, int maxPaginas,
                                  String arquivosUrl, int anosDeImportacao) {

    public CamaraApiProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://dadosabertos.camara.leg.br/api/v2";
        }
        if (timeout == null) {
            timeout = Duration.ofSeconds(30);
        }
        if (maxPaginas <= 0) {
            maxPaginas = 20;
        }
        if (arquivosUrl == null || arquivosUrl.isBlank()) {
            arquivosUrl = "https://dadosabertos.camara.leg.br/arquivos";
        }
        if (anosDeImportacao <= 0) {
            anosDeImportacao = 2;
        }
    }
}
