package com.deverdecasa.integracao.camara;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * O contrato da Câmara não é uniforme: a coleção principal exige paginação e os sub-recursos de
 * votos e autores a recusam com HTTP 400, devolvendo a lista inteira de uma vez. Errar isso não
 * quebra o build — só faz a sincronização registrar votação nenhuma —, então fica travado aqui.
 */
class CamaraApiClientTest {

    private static final String BASE = "https://dadosabertos.camara.leg.br/api/v2";

    private MockRestServiceServer servidor;
    private CamaraApiClient client;

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        servidor = MockRestServiceServer.bindTo(builder).build();
        client = new CamaraApiClient(builder.build(),
                new CamaraApiProperties(BASE, Duration.ofSeconds(5), 20));
    }

    @Test
    void votosVaoSemParametroDePaginacao() {
        servidor.expect(requestTo(BASE + "/votacoes/2460010-43/votos"))
                .andRespond(withSuccess("""
                        {"dados":[{"tipoVoto":"Sim","dataRegistroVoto":"2024-12-19T21:31:37",
                          "deputado_":{"id":178895,"nome":"Misael Varella","siglaPartido":"PSD","siglaUf":"MG"}}],
                         "links":[]}
                        """, MediaType.APPLICATION_JSON));

        var votos = client.listarVotos("2460010-43");

        servidor.verify();
        assertThat(votos).hasSize(1);
        assertThat(votos.getFirst().deputado().nome()).isEqualTo("Misael Varella");
    }

    @Test
    void autoresVaoSemParametroDePaginacao() {
        servidor.expect(requestTo(BASE + "/proposicoes/2642134/autores"))
                .andRespond(withSuccess("""
                        {"dados":[{"nome":"Acácio Favacho","tipo":"Deputado","ordemAssinatura":1,"proponente":1}],
                         "links":[]}
                        """, MediaType.APPLICATION_JSON));

        var autores = client.listarAutoresDaProposicao("2642134");

        servidor.verify();
        assertThat(autores).hasSize(1);
    }

    @Test
    void listagemDeDeputadosVaiPaginadaEParaSemLinkNext() {
        servidor.expect(requestTo(BASE + "/deputados?pagina=1&itens=100&ordem=ASC&ordenarPor=nome"))
                .andRespond(withSuccess("""
                        {"dados":[{"id":204379,"nome":"Acácio Favacho","siglaPartido":"MDB","siglaUf":"AP"}],
                         "links":[{"rel":"self","href":"..."}]}
                        """, MediaType.APPLICATION_JSON));

        var deputados = client.listarDeputados();

        servidor.verify();
        assertThat(deputados).hasSize(1);
    }

    @Test
    void listagemSegueEnquantoHouverLinkNext() {
        servidor.expect(requestTo(BASE + "/deputados?pagina=1&itens=100&ordem=ASC&ordenarPor=nome"))
                .andRespond(withSuccess("""
                        {"dados":[{"id":1,"nome":"Primeiro"}],
                         "links":[{"rel":"next","href":"..."}]}
                        """, MediaType.APPLICATION_JSON));
        servidor.expect(requestTo(BASE + "/deputados?pagina=2&itens=100&ordem=ASC&ordenarPor=nome"))
                .andRespond(withSuccess("""
                        {"dados":[{"id":2,"nome":"Segundo"}],
                         "links":[{"rel":"self","href":"..."}]}
                        """, MediaType.APPLICATION_JSON));

        var deputados = client.listarDeputados();

        servidor.verify();
        assertThat(deputados).hasSize(2);
    }
}
