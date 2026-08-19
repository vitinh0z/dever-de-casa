package com.deverdecasa.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.deverdecasa.TestePostgres;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * A busca por nome é a porta de entrada do site, então precisa aguentar nome digitado errado, sem
 * acento e pela metade.
 */
class BuscaPorNomeTest extends TestePostgres {

    private static final String CONDICAO_DE_NOME = """
            SELECT p.id FROM parlamentar p
            WHERE normaliza_nome(p.nome) LIKE '%' || normaliza_nome('favacho') || '%'
               OR normaliza_nome(p.nome) % normaliza_nome('favacho')
            """;

    @Autowired
    ParlamentarRepository repository;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void semear() {
        jdbc.update("DELETE FROM proposicao_autor");
        jdbc.update("DELETE FROM proposicao");
        jdbc.update("DELETE FROM parlamentar");
        jdbc.update("""
                INSERT INTO parlamentar (casa, id_externo, nome, sigla_uf, atualizado_em) VALUES
                    ('CAMARA', '204379', 'Acácio Favacho',  'AP', now()),
                    ('CAMARA', '204380', 'José Guimarães',  'CE', now()),
                    ('SENADO', '5672',   'Alan Rick',       'AC', now())
                """);
    }

    @Test
    void encontraMesmoComNomeDigitadoErrado() {
        var resultado = repository.buscar("Acacio Favaco", null, null, null, PageRequest.of(0, 10));

        assertThat(resultado.getContent())
                .extracting(ParlamentarResumo::getNome)
                .contains("Acácio Favacho");
    }

    @Test
    void encontraComPoucasLetrasDigitadas() {
        var resultado = repository.buscar("guima", null, null, null, PageRequest.of(0, 10));

        assertThat(resultado.getContent())
                .extracting(ParlamentarResumo::getNome)
                .contains("José Guimarães");
    }

    @Test
    void encontraIgnorandoAcentoDigitado() {
        var resultado = repository.buscar("jose guimaraes", null, null, null, PageRequest.of(0, 10));

        assertThat(resultado.getContent())
                .extracting(ParlamentarResumo::getNome)
                .contains("José Guimarães");
    }

    @Test
    void filtraPorCasa() {
        var resultado = repository.buscar(null, null, "SENADO", null, PageRequest.of(0, 10));

        assertThat(resultado.getContent())
                .extracting(ParlamentarResumo::getNome)
                .containsExactly("Alan Rick");
    }

    /**
     * O filtro de aprovação precisa mudar o número exibido, não só o rótulo. Contar pela linha de
     * autoria em vez da proposição devolveria o total de sempre com a legenda trocada para
     * "aprovados", que é pior do que não ter filtro nenhum: afirmaria algo falso sobre o mandato.
     */
    @Test
    void contagemPorAprovacaoNaoIncluiProposicaoDeOutroEstado() {
        Long idParlamentar = jdbc.queryForObject(
                "SELECT id FROM parlamentar WHERE id_externo = '204379'", Long.class);
        criarProposicao("aprovada-1", true, idParlamentar);
        criarProposicao("reprovada-1", false, idParlamentar);
        criarProposicao("reprovada-2", false, idParlamentar);
        criarProposicao("sem-situacao", null, idParlamentar);

        assertThat(qtdDe(null, idParlamentar)).as("sem filtro conta tudo").isEqualTo(4);
        assertThat(qtdDe(true, idParlamentar)).as("só as aprovadas").isEqualTo(1);
        assertThat(qtdDe(false, idParlamentar)).as("só as não aprovadas").isEqualTo(2);
    }

    @Test
    void parlamentarSemProposicaoNoEstadoFiltradoApareceComZero() {
        Long idParlamentar = jdbc.queryForObject(
                "SELECT id FROM parlamentar WHERE id_externo = '204380'", Long.class);
        criarProposicao("so-reprovada", false, idParlamentar);

        var resultado = repository.buscar("guima", null, null, true, PageRequest.of(0, 10));

        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().getFirst().getQtdProposicoes()).isZero();
    }

    private long qtdDe(Boolean aprovadas, Long idParlamentar) {
        return repository.buscar("favacho", null, null, aprovadas, PageRequest.of(0, 10))
                .getContent().stream()
                .filter(r -> r.getId().equals(idParlamentar))
                .findFirst()
                .orElseThrow()
                .getQtdProposicoes();
    }

    private void criarProposicao(String idExterno, Boolean aprovada, Long idParlamentar) {
        jdbc.update("""
                INSERT INTO proposicao (casa, id_externo, sigla_tipo, numero, ano, aprovada, atualizado_em)
                VALUES ('CAMARA', ?, 'PL', 1, 2026, ?, now())
                """, idExterno, aprovada);
        Long idProposicao = jdbc.queryForObject(
                "SELECT id FROM proposicao WHERE id_externo = ?", Long.class, idExterno);
        jdbc.update("INSERT INTO proposicao_autor (proposicao_id, parlamentar_id, proponente) VALUES (?, ?, TRUE)",
                idProposicao, idParlamentar);
    }

    @Test
    void listaTodosQuandoNaoHaTermo() {
        var resultado = repository.buscar(null, null, null, null, PageRequest.of(0, 2));

        assertThat(resultado.getTotalElements()).isEqualTo(3);
        assertThat(resultado.getContent()).hasSize(2);
    }

    /**
     * O índice GIN trigram cobre as duas travessias da busca — o LIKE de substring e o operador
     * de similaridade —, e o EXPLAIN mostra as duas entrando por ele num BitmapOr.
     *
     * <p>A tabela é inflada de propósito: com os pouco mais de 600 parlamentares em exercício, o
     * PostgreSQL prefere varrer tudo, e acerta, porque ler 600 linhas custa menos que percorrer o
     * índice. O índice existe para o caso em que a base cresce — legislaturas passadas, suplentes,
     * histórico — e é aí que ele passa a ser escolhido.
     */
    @Test
    void planoDeExecucaoUsaOIndiceTrigramQuandoOVolumeJustifica() {
        jdbc.update("""
                INSERT INTO parlamentar (casa, id_externo, nome, sigla_uf, atualizado_em)
                SELECT 'CAMARA', 'g' || g, 'Parlamentar Fictício ' || g, 'SP', now()
                FROM generate_series(1, 20000) g
                """);
        jdbc.execute("ANALYZE parlamentar");

        String plano = String.join("\n", jdbc.queryForList("EXPLAIN " + CONDICAO_DE_NOME, String.class));

        assertThat(plano)
                .as("plano de execução da busca por nome:%n%s", plano)
                .contains("idx_parlamentar_nome_trgm")
                .doesNotContain("Seq Scan on parlamentar");
    }
}
