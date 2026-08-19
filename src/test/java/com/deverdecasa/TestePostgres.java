package com.deverdecasa;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base dos testes que precisam de banco.
 *
 * <p>O container é declarado como bean, e não com {@code @Container}: aquela anotação encerra o
 * container ao fim de cada classe de teste, enquanto o contexto do Spring segue em cache e é
 * reaproveitado pela classe seguinte, que então encontraria um banco já derrubado. Como bean, o
 * ciclo de vida do container acompanha o do contexto — sobe uma vez e vale para todas as classes.
 */
@SpringBootTest
@Import(TestePostgres.ContainerDePostgres.class)
public abstract class TestePostgres {

    @TestConfiguration(proxyBeanMethods = false)
    static class ContainerDePostgres {

        @Bean
        @ServiceConnection
        PostgreSQLContainer<?> postgres() {
            return new PostgreSQLContainer<>("postgres:16-alpine");
        }
    }
}
