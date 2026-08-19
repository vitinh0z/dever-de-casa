-- Classificacao tematica oficial da Camara e o placar agregado das votacoes.
--
-- Os temas nao sao inventados aqui: a propria casa mantem uma lista de referencia e classifica
-- cada proposicao, com um grau de relevancia. Usar essa classificacao evita que o projeto
-- interprete o conteudo das propostas por conta propria.

CREATE TABLE tema (
    id         BIGSERIAL PRIMARY KEY,
    cod_externo INTEGER      NOT NULL,
    nome       VARCHAR(200) NOT NULL,
    CONSTRAINT uk_tema_cod_externo UNIQUE (cod_externo)
);

CREATE TABLE proposicao_tema (
    id            BIGSERIAL PRIMARY KEY,
    proposicao_id BIGINT  NOT NULL,
    tema_id       BIGINT  NOT NULL,
    -- Grau atribuido pela casa: distingue o assunto central dos assuntos secundarios.
    relevancia    INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uk_proposicao_tema UNIQUE (proposicao_id, tema_id),
    CONSTRAINT fk_proposicao_tema_proposicao FOREIGN KEY (proposicao_id) REFERENCES proposicao (id) ON DELETE CASCADE,
    CONSTRAINT fk_proposicao_tema_tema FOREIGN KEY (tema_id) REFERENCES tema (id) ON DELETE CASCADE
);

CREATE INDEX idx_proposicao_tema_proposicao ON proposicao_tema (proposicao_id);
CREATE INDEX idx_proposicao_tema_tema ON proposicao_tema (tema_id);

-- Placar agregado, publicado pela casa junto da votacao. Guardado porque a soma dos votos
-- individuais nem sempre bate com o total apurado, e o numero oficial e o da casa.
ALTER TABLE votacao ADD COLUMN votos_sim INTEGER;
ALTER TABLE votacao ADD COLUMN votos_nao INTEGER;
ALTER TABLE votacao ADD COLUMN votos_outros INTEGER;

-- Titulo da proposicao decidida, como a casa o publica junto da votacao ("PL 11/2003").
ALTER TABLE votacao ADD COLUMN titulo_proposicao VARCHAR(200);
