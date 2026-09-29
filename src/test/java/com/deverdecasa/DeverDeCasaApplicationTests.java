package com.deverdecasa;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Sobe o contexto da aplicação contra um PostgreSQL descartável, o mesmo caminho que as
 * migrations Flyway e as entidades JPA percorrem em produção. Exige Docker rodando.
 */
class DeverDeCasaApplicationTests extends TestePostgres {

    @Autowired
    DataSource dataSource;

    @Test
    void contextoSobeConectadoAoPostgres() throws Exception {
        try (var conn = dataSource.getConnection()) {
            assertThat(conn.getMetaData().getDatabaseProductName()).isEqualTo("PostgreSQL");
        }
    }
}
