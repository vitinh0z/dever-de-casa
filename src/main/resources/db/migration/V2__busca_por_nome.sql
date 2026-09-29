-- Busca por nome tolerante a erro de digitacao e a acento.
--
-- pg_trgm compara trigramas, o que resolve "Acacio Favaco" contra "Acácio Favacho". Falta o
-- acento: unaccent() e STABLE, nao IMMUTABLE, e o Postgres recusa funcao nao imutavel em
-- indice. A funcao abaixo fixa o dicionario e vira imutavel, que e a forma recomendada de
-- indexar texto sem acento.

CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS unaccent;

CREATE OR REPLACE FUNCTION normaliza_nome(texto TEXT) RETURNS TEXT
    LANGUAGE sql IMMUTABLE STRICT PARALLEL SAFE AS
$$ SELECT lower(public.unaccent('public.unaccent'::regdictionary, texto)) $$;

CREATE INDEX idx_parlamentar_nome_trgm
    ON parlamentar USING GIN (normaliza_nome(nome) gin_trgm_ops);
