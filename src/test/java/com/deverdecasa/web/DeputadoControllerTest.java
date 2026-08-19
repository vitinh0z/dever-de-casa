package com.deverdecasa.web;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.deverdecasa.TestePostgres;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/** As duas telas do site, exercitadas de ponta a ponta contra o banco. */
@AutoConfigureMockMvc
class DeputadoControllerTest extends TestePostgres {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbc;

    private Long idDoParlamentar;

    @BeforeEach
    void semear() {
        jdbc.update("DELETE FROM voto_parlamentar");
        jdbc.update("DELETE FROM proposicao_autor");
        jdbc.update("DELETE FROM votacao");
        jdbc.update("DELETE FROM proposicao");
        jdbc.update("DELETE FROM parlamentar");
        jdbc.update("DELETE FROM partido");

        jdbc.update("INSERT INTO partido (sigla, nome) VALUES ('MDB', 'Movimento Democrático Brasileiro')");
        Long idPartido = jdbc.queryForObject("SELECT id FROM partido WHERE sigla = 'MDB'", Long.class);
        jdbc.update("""
                INSERT INTO parlamentar (casa, id_externo, nome, sigla_uf, partido_id, situacao, atualizado_em)
                VALUES ('CAMARA', '204379', 'Acácio Favacho', 'AP', ?, 'Exercício', now())
                """, idPartido);
        idDoParlamentar = jdbc.queryForObject(
                "SELECT id FROM parlamentar WHERE id_externo = '204379'", Long.class);

        jdbc.update("""
                INSERT INTO proposicao (casa, id_externo, sigla_tipo, numero, ano, ementa, situacao, aprovada, atualizado_em)
                VALUES ('CAMARA', '2642134', 'PL', 4916, 2026, 'Altera o Estatuto da Advocacia.',
                        'Transformado em Norma Jurídica', TRUE, now())
                """);
        Long idProposicao = jdbc.queryForObject(
                "SELECT id FROM proposicao WHERE id_externo = '2642134'", Long.class);
        jdbc.update("INSERT INTO proposicao_autor (proposicao_id, parlamentar_id, proponente) VALUES (?, ?, TRUE)",
                idProposicao, idDoParlamentar);
    }

    @Test
    void buscaAceitaNomePartidoEStatusJuntos() throws Exception {
        mockMvc.perform(get("/deputados")
                        .param("nome", "favacho")
                        .param("partido", "MDB")
                        .param("aprovadas", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("busca"))
                .andExpect(content().string(containsString("Acácio Favacho")));
    }

    @Test
    void filtroQueNaoBateNaoTrazOParlamentar() throws Exception {
        mockMvc.perform(get("/deputados").param("partido", "PT"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Nenhum parlamentar encontrado")));
    }

    @Test
    void perfilMostraProjetosAutorados() throws Exception {
        mockMvc.perform(get("/deputados/{id}", idDoParlamentar))
                .andExpect(status().isOk())
                .andExpect(view().name("perfil"))
                .andExpect(content().string(containsString("PL 4916/2026")))
                .andExpect(content().string(containsString("Aprovado")));
    }

    /**
     * Quem não tem voto nominal registrado precisa ver a explicação, e não uma lista vazia que
     * passaria a impressão de que o parlamentar se omitiu.
     */
    @Test
    void perfilSemVotoNominalExplicaEmVezDeMostrarListaVazia() throws Exception {
        mockMvc.perform(get("/deputados/{id}", idDoParlamentar))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Não há voto nominal registrado")));
    }

    @Test
    void perfilInexistenteDevolvePaginaAmigavelDe404() throws Exception {
        mockMvc.perform(get("/deputados/{id}", 999_999L))
                .andExpect(status().isNotFound())
                .andExpect(view().name("erro"))
                .andExpect(model().attributeExists("mensagem"))
                .andExpect(content().string(containsString("Não encontramos")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("Exception"))));
    }
}
