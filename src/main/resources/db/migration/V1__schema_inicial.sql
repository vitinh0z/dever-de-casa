-- Schema inicial: parlamentares das duas casas, o que eles apresentaram e como votaram.
-- A identidade de parlamentar, proposicao e votacao e sempre (casa, id_externo), porque
-- Camara e Senado numeram seus registros de forma independente e os codigos colidem.

CREATE TABLE partido (
    id    BIGSERIAL PRIMARY KEY,
    sigla VARCHAR(40) NOT NULL,
    nome  VARCHAR(200),
    CONSTRAINT uk_partido_sigla UNIQUE (sigla)
);

CREATE TABLE parlamentar (
    id                 BIGSERIAL PRIMARY KEY,
    casa               VARCHAR(20)  NOT NULL,
    id_externo         VARCHAR(40)  NOT NULL,
    nome               VARCHAR(200) NOT NULL,
    nome_civil         VARCHAR(200),
    partido_id         BIGINT,
    sigla_uf           VARCHAR(2),
    url_foto           VARCHAR(500),
    email              VARCHAR(200),
    situacao           VARCHAR(100),
    condicao_eleitoral VARCHAR(100),
    id_legislatura     INTEGER,
    atualizado_em      TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_parlamentar_casa_id_externo UNIQUE (casa, id_externo),
    CONSTRAINT fk_parlamentar_partido FOREIGN KEY (partido_id) REFERENCES partido (id)
);

CREATE TABLE proposicao (
    id               BIGSERIAL PRIMARY KEY,
    casa             VARCHAR(20) NOT NULL,
    id_externo       VARCHAR(40) NOT NULL,
    sigla_tipo       VARCHAR(20) NOT NULL,
    numero           INTEGER,
    ano              INTEGER,
    ementa           TEXT,
    data_apresentacao DATE,
    situacao         VARCHAR(300),
    -- NULL enquanto a situacao nao foi consultada: desconhecido nao e o mesmo que reprovado.
    aprovada         BOOLEAN,
    url_inteiro_teor VARCHAR(500),
    atualizado_em    TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_proposicao_casa_id_externo UNIQUE (casa, id_externo)
);

CREATE TABLE proposicao_autor (
    id               BIGSERIAL PRIMARY KEY,
    proposicao_id    BIGINT  NOT NULL,
    parlamentar_id   BIGINT  NOT NULL,
    ordem_assinatura INTEGER,
    proponente       BOOLEAN NOT NULL,
    CONSTRAINT uk_proposicao_autor UNIQUE (proposicao_id, parlamentar_id),
    CONSTRAINT fk_proposicao_autor_proposicao FOREIGN KEY (proposicao_id) REFERENCES proposicao (id) ON DELETE CASCADE,
    CONSTRAINT fk_proposicao_autor_parlamentar FOREIGN KEY (parlamentar_id) REFERENCES parlamentar (id) ON DELETE CASCADE
);

CREATE TABLE votacao (
    id            BIGSERIAL PRIMARY KEY,
    casa          VARCHAR(20) NOT NULL,
    id_externo    VARCHAR(60) NOT NULL,
    data          DATE,
    descricao     TEXT,
    sigla_orgao   VARCHAR(40),
    aprovada      BOOLEAN,
    -- Votacao simbolica nao registra voto individual; sem esse marcador uma lista vazia de
    -- votos seria lida como omissao dos parlamentares.
    nominal       BOOLEAN NOT NULL,
    secreta       BOOLEAN NOT NULL,
    proposicao_id BIGINT,
    CONSTRAINT uk_votacao_casa_id_externo UNIQUE (casa, id_externo),
    CONSTRAINT fk_votacao_proposicao FOREIGN KEY (proposicao_id) REFERENCES proposicao (id) ON DELETE SET NULL
);

CREATE TABLE voto_parlamentar (
    id               BIGSERIAL PRIMARY KEY,
    votacao_id       BIGINT      NOT NULL,
    parlamentar_id   BIGINT      NOT NULL,
    tipo_voto        VARCHAR(20) NOT NULL,
    descricao_origem VARCHAR(100),
    data_registro    TIMESTAMP(6) WITH TIME ZONE,
    CONSTRAINT uk_voto_parlamentar UNIQUE (votacao_id, parlamentar_id),
    CONSTRAINT fk_voto_votacao FOREIGN KEY (votacao_id) REFERENCES votacao (id) ON DELETE CASCADE,
    CONSTRAINT fk_voto_parlamentar FOREIGN KEY (parlamentar_id) REFERENCES parlamentar (id) ON DELETE CASCADE
);

-- Indices das travessias que as telas fazem: perfil de um parlamentar e filtro por partido/casa.
CREATE INDEX idx_parlamentar_partido ON parlamentar (partido_id);
CREATE INDEX idx_parlamentar_casa ON parlamentar (casa);
CREATE INDEX idx_proposicao_autor_parlamentar ON proposicao_autor (parlamentar_id);
CREATE INDEX idx_proposicao_autor_proposicao ON proposicao_autor (proposicao_id);
CREATE INDEX idx_proposicao_aprovada ON proposicao (aprovada);
CREATE INDEX idx_voto_parlamentar_parlamentar ON voto_parlamentar (parlamentar_id);
CREATE INDEX idx_voto_parlamentar_votacao ON voto_parlamentar (votacao_id);
CREATE INDEX idx_votacao_proposicao ON votacao (proposicao_id);
