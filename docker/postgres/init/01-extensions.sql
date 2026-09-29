-- Busca por nome de parlamentar tolerante a acento e grafia parcial (issue #6).
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS unaccent;
