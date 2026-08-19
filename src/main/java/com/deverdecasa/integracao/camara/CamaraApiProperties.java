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
 */
@ConfigurationProperties(prefix = "deverdecasa.camara")
public record CamaraApiProperties(String baseUrl, Duration timeout, int maxPaginas) {

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
    }
}
