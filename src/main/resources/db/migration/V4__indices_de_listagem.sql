-- Indices que sustentam a contagem de projetos exibida na listagem.
--
-- A tela conta, por parlamentar, quantas proposicoes ele assinou, e o filtro de aprovacao entra
-- nessa conta. Sem os indices abaixo, cada carregamento percorria as autorias e depois a tabela
-- de proposicoes linha a linha: com a base cheia a listagem filtrada levava segundos.
--
-- O par (parlamentar_id, proposicao_id) permite resolver a contagem so pelo indice, sem visitar a
-- tabela de autoria, e o par (aprovada, id) faz o mesmo do lado da proposicao.

CREATE INDEX idx_proposicao_autor_par_prop ON proposicao_autor (parlamentar_id, proposicao_id);
CREATE INDEX idx_proposicao_aprovada_id ON proposicao (aprovada, id);
