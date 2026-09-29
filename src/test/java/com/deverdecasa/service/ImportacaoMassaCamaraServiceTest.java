package com.deverdecasa.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.deverdecasa.TestePostgres;
import com.deverdecasa.integracao.camara.ArquivosCamaraClient;
import java.time.Year;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * A carga em massa é o caminho que enche a base, e falha nela não aparece como erro: aparece como
 * um site com menos dados do que deveria. Os recortes abaixo reproduzem o formato de cada arquivo
 * publicado pela Câmara.
 */
class ImportacaoMassaCamaraServiceTest extends TestePostgres {

    private static final String BASE = "https://dadosabertos.camara.leg.br/arquivos";
    private static final int ANO = Year.now().getValue();

    @Autowired
    JdbcTemplate jdbc;

    private MockRestServiceServer servidor;
    private ImportacaoMassaCamaraService importacao;

    @BeforeEach
    void preparar() {
        jdbc.update("DELETE FROM voto_parlamentar");
        jdbc.update("DELETE FROM proposicao_tema");
        jdbc.update("DELETE FROM proposicao_autor");
        jdbc.update("DELETE FROM votacao");
        jdbc.update("DELETE FROM proposicao");
        jdbc.update("DELETE FROM tema");
        jdbc.update("DELETE FROM parlamentar");

        jdbc.update("""
                INSERT INTO parlamentar (casa, id_externo, nome, sigla_uf, atualizado_em)
                VALUES ('CAMARA', '204379', 'Acácio Favacho', 'AP', now()),
                       ('CAMARA', '204501', 'Alencar Santana', 'SP', now())
                """);

        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        servidor = MockRestServiceServer.bindTo(builder).build();
        importacao = new ImportacaoMassaCamaraService(new ArquivosCamaraClient(builder.build()), jdbc);
        responderArquivos();
    }

    /**
     * As respostas são registradas na sequência em que a importação lê os arquivos, que é também
     * a ordem de dependência entre eles: proposição antes de autoria, votação antes de voto.
     */
    private void responderArquivos() {
        List<Map.Entry<String, String>> arquivos = List.of(
                Map.entry("/proposicoes/json/proposicoes-%d.json".formatted(ANO), """
                        [{"id":2642134,"siglaTipo":"PL","numero":4916,"ano":2026,
                          "ementa":"Altera o Estatuto da Advocacia.","dataApresentacao":"2026-08-06T13:17",
                          "urlInteiroTeor":"https://exemplo/inteiro-teor",
                          "ultimoStatus":{"descricaoSituacao":"Transformado em Norma Jurídica"}},
                         {"id":2642135,"siglaTipo":"PL","numero":4917,"ano":2026,
                          "ementa":"Outro projeto.","dataApresentacao":"2026-08-07",
                          "ultimoStatus":{"descricaoSituacao":"Arquivada"}}]
                        """),
                Map.entry("/proposicoesAutores/json/proposicoesAutores-%d.json".formatted(ANO), """
                        [{"idProposicao":2642134,"idDeputadoAutor":204379,"tipoAutor":"Deputado(a)",
                          "ordemAssinatura":"1","proponente":"1"},
                         {"idProposicao":2642134,"idDeputadoAutor":204501,"tipoAutor":"Deputado(a)",
                          "ordemAssinatura":"2","proponente":"0"},
                         {"idProposicao":2642134,"idDeputadoAutor":null,"tipoAutor":"Órgão do Poder Executivo",
                          "nomeAutor":"Poder Executivo","ordemAssinatura":"3","proponente":"0"}]
                        """),
                Map.entry("/proposicoesTemas/json/proposicoesTemas-%d.json".formatted(ANO), """
                        [{"uriProposicao":"https://dadosabertos.camara.leg.br/api/v2/proposicoes/2642134",
                          "codTema":76,"tema":"Direito e Justiça","relevancia":1},
                         {"uriProposicao":"https://dadosabertos.camara.leg.br/api/v2/proposicoes/2642134",
                          "codTema":34,"tema":"Administração Pública","relevancia":0}]
                        """),
                Map.entry("/votacoes/json/votacoes-%d.json".formatted(ANO), """
                        [{"id":"2642134-10","data":"2026-08-10","siglaOrgao":"PLEN",
                          "descricao":"Rejeitada a Emenda de Plenário nº 1.","aprovacao":0,
                          "votosSim":120,"votosNao":300,"votosOutros":5},
                         {"id":"2642134-11","data":"2026-08-11","siglaOrgao":"PLEN",
                          "descricao":"Aprovado o Projeto.","aprovacao":1,
                          "votosSim":0,"votosNao":0,"votosOutros":0}]
                        """),
                Map.entry("/votacoesProposicoes/json/votacoesProposicoes-%d.json".formatted(ANO), """
                        [{"idVotacao":"2642134-10","proposicao_":{"id":2642134,"titulo":"PL 4916/2026",
                          "ementa":"Altera o Estatuto da Advocacia.","siglaTipo":"PL","numero":4916,"ano":2026}}]
                        """),
                Map.entry("/votacoesVotos/json/votacoesVotos-%d.json".formatted(ANO), """
                        [{"idVotacao":"2642134-10","dataHoraVoto":"2026-08-10T14:46:15","voto":"Não",
                          "deputado_":{"id":"204501","nome":"Alencar Santana"}},
                         {"idVotacao":"2642134-10","dataHoraVoto":"2026-08-10T14:46:20","voto":"Sim",
                          "deputado_":{"id":"204379","nome":"Acácio Favacho"}},
                         {"idVotacao":"2642134-10","dataHoraVoto":"2026-08-10T14:47:00","voto":"Sim",
                          "deputado_":{"id":"999999","nome":"Deputado fora da base"}}]
                        """));

        arquivos.forEach(arquivo ->
                servidor.expect(requestTo(BASE + arquivo.getKey()))
                        .andRespond(withSuccess(arquivo.getValue(), MediaType.APPLICATION_JSON)));
    }

    @Test
    void importaOAnoInteiroDosArquivos() {
        importacao.importarAno(ANO);

        assertThat(contar("proposicao")).isEqualTo(2);
        assertThat(contar("tema")).isEqualTo(2);
        assertThat(contar("proposicao_tema")).isEqualTo(2);
        assertThat(contar("votacao")).isEqualTo(2);
    }

    /**
     * O número relatado precisa bater com o que foi gravado. Contar antes de o último lote ser
     * descarregado devolvia zero para arquivos menores que o lote, e a importação terminava
     * dizendo que nada tinha entrado enquanto o banco enchia normalmente — falha que passa
     * despercebida justamente por não quebrar nada.
     */
    @Test
    void relataAQuantidadeQueRealmenteGravou() {
        ResultadoSincronizacao resultado = importacao.importarAno(ANO);

        assertThat(resultado.proposicoes()).isEqualTo(2);
        assertThat(resultado.votacoes()).isEqualTo(2);
        assertThat(resultado.votos()).isEqualTo(3);
    }

    @Test
    void distingueProponenteDeCoautorEIgnoraAutorQueNaoEParlamentar() {
        importacao.importarAno(ANO);

        assertThat(contar("proposicao_autor"))
                .as("o Poder Executivo assina, mas não é parlamentar")
                .isEqualTo(2);
        assertThat(jdbc.queryForObject("""
                SELECT pa.proponente FROM proposicao_autor pa
                JOIN parlamentar p ON p.id = pa.parlamentar_id
                WHERE p.id_externo = '204379'
                """, Boolean.class)).isTrue();
        assertThat(jdbc.queryForObject("""
                SELECT pa.proponente FROM proposicao_autor pa
                JOIN parlamentar p ON p.id = pa.parlamentar_id
                WHERE p.id_externo = '204501'
                """, Boolean.class)).isFalse();
    }

    /** Sem o vínculo, a tela só teria "Rejeitada a Emenda de Plenário nº 1" solto. */
    @Test
    void ligaVotacaoAProposicaoQueElaDecide() {
        importacao.importarAno(ANO);

        assertThat(jdbc.queryForObject("""
                SELECT pr.id_externo FROM votacao vt
                JOIN proposicao pr ON pr.id = vt.proposicao_id
                WHERE vt.id_externo = '2642134-10'
                """, String.class)).isEqualTo("2642134");
        assertThat(jdbc.queryForObject(
                "SELECT titulo_proposicao FROM votacao WHERE id_externo = '2642134-10'", String.class))
                .isEqualTo("PL 4916/2026");
    }

    @Test
    void gravaVotosEMarcaApenasAVotacaoNominal() {
        importacao.importarAno(ANO);

        assertThat(contar("voto_parlamentar"))
                .as("voto de deputado fora da base é descartado, não vira registro órfão")
                .isEqualTo(2);
        assertThat(jdbc.queryForObject(
                "SELECT nominal FROM votacao WHERE id_externo = '2642134-10'", Boolean.class)).isTrue();
        assertThat(jdbc.queryForObject(
                "SELECT nominal FROM votacao WHERE id_externo = '2642134-11'", Boolean.class))
                .as("votação sem voto individual continua simbólica")
                .isFalse();
    }

    @Test
    void classificaAprovacaoPelaSituacaoPublicada() {
        importacao.importarAno(ANO);

        assertThat(jdbc.queryForObject(
                "SELECT aprovada FROM proposicao WHERE id_externo = '2642134'", Boolean.class)).isTrue();
        assertThat(jdbc.queryForObject(
                "SELECT aprovada FROM proposicao WHERE id_externo = '2642135'", Boolean.class)).isFalse();
    }

    @Test
    void guardaOPlacarPublicadoPelaCasa() {
        importacao.importarAno(ANO);

        assertThat(jdbc.queryForObject(
                "SELECT votos_sim FROM votacao WHERE id_externo = '2642134-10'", Integer.class)).isEqualTo(120);
        assertThat(jdbc.queryForObject(
                "SELECT votos_nao FROM votacao WHERE id_externo = '2642134-10'", Integer.class)).isEqualTo(300);
    }

    private long contar(String tabela) {
        return jdbc.queryForObject("SELECT count(*) FROM " + tabela, Long.class);
    }
}
