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
