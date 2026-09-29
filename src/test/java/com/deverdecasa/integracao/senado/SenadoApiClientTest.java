package com.deverdecasa.integracao.senado;

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
 * As amostras abaixo são recortes das respostas reais do Senado.
 *
 * <p>O envelope de senadores é o ponto frágil: são três níveis de aninhamento em PascalCase, e
 * qualquer erro de mapeamento devolve uma lista vazia em silêncio, sem falhar nada — a
 * sincronização terminaria dizendo que a casa não tem senador nenhum.
 */
class SenadoApiClientTest {

    private static final String BASE = "https://legis.senado.leg.br/dadosabertos";

    private MockRestServiceServer servidor;
    private RestClient.Builder builder;

    @BeforeEach
    void preparar() {
        builder = RestClient.builder().baseUrl(BASE);
        servidor = MockRestServiceServer.bindTo(builder).build();
    }

    private SenadoApiClient client(int anosDeHistorico) {
        return new SenadoApiClient(builder.build(),
                new SenadoApiProperties(BASE, Duration.ofSeconds(5), anosDeHistorico));
    }

    @Test
    void desembrulhaOEnvelopeDeSenadores() {
        servidor.expect(requestTo(BASE + "/senador/lista/atual.json"))
                .andRespond(withSuccess("""
                        {"ListaParlamentarEmExercicio":{
                          "Parlamentares":{"Parlamentar":[
                            {"IdentificacaoParlamentar":{
                              "CodigoParlamentar":"5672",
                              "NomeParlamentar":"Alan Rick",
                              "NomeCompletoParlamentar":"Alan Rick Miranda",
                              "SiglaPartidoParlamentar":"REPUBLICANOS",
                              "UfParlamentar":"AC",
                              "UrlFotoParlamentar":"http://www.senado.leg.br/senadores/img/senador5672.jpg",
                              "EmailParlamentar":"sen.alanrick@senado.leg.br"}}]}}}
                        """, MediaType.APPLICATION_JSON));

        var senadores = client(1).listarSenadores();

        servidor.verify();
        assertThat(senadores).hasSize(1);
        var alan = senadores.getFirst();
        assertThat(alan.codigoParlamentar()).isEqualTo("5672");
        assertThat(alan.nomeParlamentar()).isEqualTo("Alan Rick");
        assertThat(alan.nomeCompleto()).isEqualTo("Alan Rick Miranda");
        assertThat(alan.siglaPartido()).isEqualTo("REPUBLICANOS");
        assertThat(alan.uf()).isEqualTo("AC");
    }

    @Test
    void leVotacaoComVotosEmbutidos() {
        servidor.expect(requestTo(BASE + "/votacao?ano=" + java.time.Year.now().getValue()))
                .andRespond(withSuccess("""
                        [{"codigoSessaoVotacao":6918,
                          "identificacao":"PLP 22/2025",
                          "descricaoVotacao":"Votação nominal da Emenda nº 1.",
                          "dataSessao":"2025-02-19",
                          "resultadoVotacao":"A",
                          "votacaoSecreta":"N",
                          "votos":[
                            {"codigoParlamentar":5672,"nomeParlamentar":"Alan Rick","siglaVotoParlamentar":"Sim"},
                            {"codigoParlamentar":1234,"nomeParlamentar":"Outro","siglaVotoParlamentar":"Não"}]}]
                        """, MediaType.APPLICATION_JSON));

        var votacoes = client(1).listarVotacoes();

        servidor.verify();
        assertThat(votacoes).hasSize(1);
        var votacao = votacoes.getFirst();
        assertThat(votacao.aprovada()).isTrue();
        assertThat(votacao.secreta()).isFalse();
        assertThat(votacao.votosOuVazio()).hasSize(2);
    }

    @Test
    void marcaVotacaoSecreta() {
        servidor.expect(requestTo(BASE + "/votacao?ano=" + java.time.Year.now().getValue()))
                .andRespond(withSuccess("""
                        [{"codigoSessaoVotacao":1,"votacaoSecreta":"S","resultadoVotacao":"A",
                          "votos":[{"codigoParlamentar":5672,"siglaVotoParlamentar":"Votou"}]}]
                        """, MediaType.APPLICATION_JSON));

        var votacao = client(1).listarVotacoes().getFirst();

        assertThat(votacao.secreta()).isTrue();
        assertThat(votacao.votosOuVazio().getFirst().siglaVotoParlamentar()).isEqualTo("Votou");
    }

    @Test
    void leMateriasDoAutorAnoAAno() {
        int ano = java.time.Year.now().getValue();
        servidor.expect(requestTo(BASE + "/processo?codigoParlamentarAutor=5672&ano=" + ano))
                .andRespond(withSuccess("""
                        [{"id":8784639,"identificacao":"RQS 19/2025","ementa":"Requer sessão especial.",
                          "dataApresentacao":"2025-01-10","situacaoAtual":"SESSÃO REALIZADA",
                          "siglaTipoDeliberacao":"APROVADA_NO_PLENARIO"}]
                        """, MediaType.APPLICATION_JSON));
        servidor.expect(requestTo(BASE + "/processo?codigoParlamentarAutor=5672&ano=" + (ano - 1)))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        var materias = client(2).listarMateriasPorAutor("5672");

        servidor.verify();
        assertThat(materias).hasSize(1);
        assertThat(materias.getFirst().siglaTipoDeliberacao()).isEqualTo("APROVADA_NO_PLENARIO");
    }
}
