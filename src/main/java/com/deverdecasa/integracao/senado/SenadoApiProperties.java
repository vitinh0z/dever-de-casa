package com.deverdecasa.integracao.senado;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Ajustes da integração com o Senado.
 *
 * @param baseUrl     raiz da API de Dados Abertos
 * @param timeout     corte de paciência por requisição
 * @param anosDeHistorico quantos anos para trás buscar matérias e votações
 */
@ConfigurationProperties(prefix = "deverdecasa.senado")
public record SenadoApiProperties(String baseUrl, Duration timeout, int anosDeHistorico) {

    public SenadoApiProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://legis.senado.leg.br/dadosabertos";
        }
        if (timeout == null) {
            timeout = Duration.ofSeconds(45);
        }
        if (anosDeHistorico <= 0) {
            anosDeHistorico = 2;
        }
    }
}
