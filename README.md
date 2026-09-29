# dever de casa

Todo mandato eletivo vem com um dever de casa: legislar em nome de quem votou. Na prática, é quase impossível pro cidadão comum acompanhar isso.

## O problema

As informações existem — a Câmara dos Deputados e o Senado Federal publicam dados abertos sobre proposições, autores e votações. Mas estão espalhadas em APIs distintas, formatos diferentes e sem nenhuma interface pensada para quem não é desenvolvedor ou jornalista especializado.

## A ideia

Reunir num único lugar as informações públicas sobre a atividade legislativa de deputados federais e, futuramente, senadores, de forma simples de navegar e entender.

Cada parlamentar tem seu próprio perfil, com:

- Nome, partido e estado
- Quantidade de projetos de lei que já apresentou
- Quais propostas ele efetivamente autorou, não apenas discursou sobre

Além do perfil individual, o projeto acompanha o que está em tramitação nas duas casas: quais projetos estão circulando no momento, em que fase do processo legislativo se encontram, e — quando a votação é nominal — quais parlamentares votaram a favor e quais votaram contra. Isso transforma uma decisão coletiva abstrata em algo rastreável até o nome de quem a tomou.

## O que isso não é

Não é um lugar pra fazer juízo de valor sobre nenhum parlamentar, partido ou projeto. É só organizar dado público que já existe, mas que hoje exige tempo e conhecimento técnico pra ser acessado.

A meta final é simples: qualquer pessoa abre o site, procura pelo nome do seu deputado ou senador, e entende rapidamente o que essa pessoa andou fazendo com o mandato que recebeu.

## De onde vêm os dados

APIs de dados abertos da Câmara dos Deputados e do Senado Federal, mantidas pelas próprias casas legislativas. O escopo inicial é a Câmara, por ter uma API mais completa e unificada; o Senado entra numa fase posterior, já que segue um modelo de dados próprio.

## Como rodar

Você precisa de JDK 21, Maven 3.9+ e Docker.

```bash
mvn spring-boot:run
```

O Postgres sobe junto pelo `docker-compose.yml`, já com a extensão `pg_trgm` habilitada — não é preciso subir nada à parte. A aplicação fica em `http://localhost:8080`.

Se a porta 5432 já estiver ocupada na sua máquina, crie um `.env` na raiz com `POSTGRES_PORT=5433` (ou outra livre); o resto se ajusta sozinho.

Os testes usam um Postgres descartável via Testcontainers, então também pedem Docker:

```bash
mvn verify
```

## Contribuindo

Quer ajudar? O fluxo de contribuição está descrito em [CONTRIBUTING.md](CONTRIBUTING.md), e o trabalho em andamento está organizado nas [issues do repositório](https://github.com/vitinh0z/dever-de-casa/issues).

## Licença

Distribuído sob a licença [MIT](LICENSE).
