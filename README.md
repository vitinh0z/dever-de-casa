# dever de casa

Todo mandato eletivo vem com um dever de casa: legislar em nome de quem votou. Na prática, é quase impossível pro cidadão comum acompanhar isso. As informações existem — a Câmara dos Deputados e o Senado Federal publicam dados abertos sobre proposições, autores e votações — mas estão espalhadas em APIs distintas, formatos diferentes e sem nenhuma interface pensada para quem não é desenvolvedor ou jornalista especializado.

O dever de casa nasce pra resolver esse problema: reunir num único lugar as informações públicas sobre a atividade legislativa de deputados federais e, futuramente, senadores, de forma simples de navegar e entender.

A ideia central é dar ao cidadão um ambiente unificado por parlamentar. Cada deputado e cada senador tem seu próprio perfil, mostrando nome, partido e estado, além do volume de projetos de lei que já apresentou. A partir desse perfil dá pra ver quais propostas aquele parlamentar efetivamente autorou, não apenas discursou sobre.

Além do perfil individual, o projeto acompanha o que está em tramitação nas duas casas: quais projetos estão circulando no momento, em que fase do processo legislativo se encontram, e, quando a votação é nominal (ou seja, registrada voto a voto), quais parlamentares votaram a favor e quais votaram contra. Isso transforma uma decisão coletiva abstrata em algo rastreável até o nome de quem a tomou.

O objetivo não é fazer juízo de valor sobre nenhum parlamentar, partido ou projeto — é só organizar dado público que já existe, mas que hoje exige tempo e conhecimento técnico pra ser acessado. A meta final é que qualquer pessoa consiga abrir o site, procurar pelo nome do seu deputado ou senador, e entender rapidamente o que essa pessoa andou fazendo com o mandato que recebeu.

Os dados vêm das APIs de dados abertos da Câmara dos Deputados e do Senado Federal, mantidas pelas próprias casas legislativas. O escopo inicial é a Câmara dos Deputados, por ter uma API mais completa e unificada; o Senado entra numa fase posterior, já que segue um modelo de dados próprio e exige tratamento separado.

## Stack

- Java 21 + Spring Boot (Web, Data JPA, Thymeleaf, Validation)
- PostgreSQL, com Flyway para versionamento de schema e índice trigram (`pg_trgm`) para busca por nome tolerante a erro de digitação
- Frontend server-side rendered em HTML/CSS puro via Thymeleaf, sem framework de JavaScript
- Sincronização de dados via job agendado (`@Scheduled`), que consulta a API da Câmara periodicamente e mantém o banco local atualizado — o usuário nunca bate direto na API do governo

O código é organizado em camadas: `web` (controllers), `dto`, `service`, `mapper`, `domain` (entidades JPA), `repository` e `integracao` (clientes das APIs externas e seus DTOs de resposta bruta). Controllers nunca acessam repositórios diretamente, e entidades JPA não cruzam a fronteira da camada de serviço — quem atravessa é sempre um DTO.

## Como rodar localmente

Pré-requisitos: JDK 21, Maven e um PostgreSQL acessível localmente (com a extensão `pg_trgm` habilitada), com as credenciais batendo com `src/main/resources/application.yml`.

```
mvn spring-boot:run
```

A aplicação sobe em `http://localhost:8080`.

## Estrutura do projeto

```
src/main/java/com/deverdecasa/
├── web/           controllers
├── dto/           objetos que trafegam entre web e service
├── service/       regras de negócio e orquestração da sincronização
├── mapper/        tradução entre entidade, DTO e resposta da API
├── domain/        entidades JPA
├── repository/    interfaces Spring Data
└── integracao/    clientes HTTP e DTOs das APIs externas

src/main/resources/
├── templates/     páginas Thymeleaf
├── static/css/    estilos
└── db/migration/  migrations Flyway
```

## Contribuindo

O fluxo de contribuição, padrão de branches e convenções de código estão descritos em [CONTRIBUTING.md](CONTRIBUTING.md). O trabalho em andamento está organizado nas [issues do repositório](https://github.com/vitinh0z/dever-de-casa/issues), agrupadas em épicos com suas respectivas sub-issues de implementação.

## Licença

Distribuído sob a licença [MIT](LICENSE).
