package com.deverdecasa.integracao;

import com.deverdecasa.integracao.camara.CamaraApiProperties;
import com.deverdecasa.integracao.senado.SenadoApiProperties;
import java.time.Duration;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

/** Clientes HTTP das casas legislativas, um por API, sem estado compartilhado entre elas. */
@Configuration
public class IntegracaoConfig {

    @Bean
    RestClient camaraRestClient(RestClient.Builder builder, CamaraApiProperties properties) {
        return construir(builder, properties.baseUrl(), properties.timeout());
    }

    @Bean
    RestClient senadoRestClient(RestClient.Builder builder, SenadoApiProperties properties) {
        return construir(builder, properties.baseUrl(), properties.timeout());
    }

    private RestClient construir(RestClient.Builder builder, String baseUrl, Duration timeout) {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(timeout)
                .withReadTimeout(timeout);
        return builder.clone()
                .baseUrl(baseUrl)
                .requestFactory(ClientHttpRequestFactories.get(settings))
                .defaultHeader(HttpHeaders.ACCEPT, "application/json")
                .defaultHeader(HttpHeaders.USER_AGENT, "dever-de-casa (github.com/vitinh0z/dever-de-casa)")
                .build();
    }
}
