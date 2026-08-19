package com.deverdecasa.service;

import com.deverdecasa.integracao.camara.ArquivosCamaraClient;
import com.deverdecasa.integracao.camara.ArquivosCamaraClient.ArquivoEmMassa;
import com.deverdecasa.integracao.camara.ArquivosDtos.AutoriaEmMassa;
import com.deverdecasa.integracao.camara.ArquivosDtos.ProposicaoEmMassa;
import com.deverdecasa.integracao.camara.ArquivosDtos.TemaEmMassa;
import com.deverdecasa.integracao.camara.ArquivosDtos.VotacaoEmMassa;
import com.deverdecasa.integracao.camara.ArquivosDtos.VotacaoProposicaoEmMassa;
import com.deverdecasa.integracao.camara.ArquivosDtos.VotoEmMassa;
import com.deverdecasa.domain.TipoVoto;
import com.deverdecasa.mapper.Datas;
import com.deverdecasa.mapper.SituacaoProposicao;
import java.sql.Types;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Carga inicial da Câmara a partir dos arquivos publicados em bloco.
 *
 * <p>Escreve com SQL direto, e não pelo mapeamento objeto-relacional, porque aqui não há regra de
 * negócio por registro: são dezenas de milhares de linhas indo para o banco em lote, caso em que
 * o ORM só acrescentaria o custo de manter cada uma delas em contexto de persistência. Cada
 * gravação é um "insere ou atualiza" apoiado nas chaves naturais que o schema já declara, então
 * repetir a importação corrige o que mudou em vez de duplicar.
 */
@Service
public class ImportacaoMassaCamaraService {

    private static final Logger log = LoggerFactory.getLogger(ImportacaoMassaCamaraService.class);
    private static final int TAMANHO_DO_LOTE = 500;
    private static final String CAMARA = "CAMARA";

    private final ArquivosCamaraClient arquivos;
    private final JdbcTemplate jdbc;

    public ImportacaoMassaCamaraService(ArquivosCamaraClient arquivos, JdbcTemplate jdbc) {
        this.arquivos = arquivos;
        this.jdbc = jdbc;
    }

    /**
     * Importa um ano inteiro. A ordem importa: proposições antes de autorias e temas, votações
     * antes dos votos, porque cada etapa se apoia nas chaves gravadas pela anterior.
     */
    public ResultadoSincronizacao importarAno(int ano) {
        log.info("Importando dados abertos da Câmara do ano {}", ano);
        long proposicoes = importarProposicoes(ano);
        long autorias = importarAutorias(ano);
        long temas = importarTemas(ano);
        long votacoes = importarVotacoes(ano);
        long vinculos = importarVinculoVotacaoProposicao(ano);
        long votos = importarVotos(ano);

        log.info("Ano {}: {} proposições, {} autorias, {} temas, {} votações, {} vínculos, {} votos",
                ano, proposicoes, autorias, temas, votacoes, vinculos, votos);
        return new ResultadoSincronizacao(0, (int) proposicoes, (int) votacoes, (int) votos, 0);
    }

    private long importarProposicoes(int ano) {
        GravadorEmLote<ProposicaoEmMassa> lote = new GravadorEmLote<>(TAMANHO_DO_LOTE, this::gravarProposicoes);
        try (lote) {
            arquivos.lerAno(ArquivoEmMassa.PROPOSICOES, ano, ProposicaoEmMassa.class, lote::adicionar);
        }
        return lote.total();
    }

    private void gravarProposicoes(List<ProposicaoEmMassa> registros) {
        jdbc.batchUpdate("""
                INSERT INTO proposicao (casa, id_externo, sigla_tipo, numero, ano, ementa,
                                        data_apresentacao, situacao, aprovada, url_inteiro_teor, atualizado_em)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now())
                ON CONFLICT (casa, id_externo) DO UPDATE SET
                    sigla_tipo = EXCLUDED.sigla_tipo,
                    numero = EXCLUDED.numero,
                    ano = EXCLUDED.ano,
                    ementa = EXCLUDED.ementa,
                    data_apresentacao = EXCLUDED.data_apresentacao,
                    situacao = EXCLUDED.situacao,
                    aprovada = EXCLUDED.aprovada,
                    url_inteiro_teor = EXCLUDED.url_inteiro_teor,
                    atualizado_em = now()
                """, registros, registros.size(), (ps, r) -> {
            String situacao = r.situacao();
            ps.setString(1, CAMARA);
            ps.setString(2, String.valueOf(r.id()));
            ps.setString(3, r.siglaTipo() == null ? "?" : r.siglaTipo());
            setInteiro(ps, 4, r.numero());
            setInteiro(ps, 5, r.ano());
            ps.setString(6, r.ementa());
            setData(ps, 7, Datas.paraData(r.dataApresentacao()));
            ps.setString(8, situacao);
            setBooleano(ps, 9, SituacaoProposicao.aprovada(situacao));
            ps.setString(10, r.urlInteiroTeor());
        });
    }

    private long importarAutorias(int ano) {
        GravadorEmLote<AutoriaEmMassa> lote = new GravadorEmLote<>(TAMANHO_DO_LOTE, this::gravarAutorias);
        try (lote) {
            arquivos.lerAno(ArquivoEmMassa.PROPOSICOES_AUTORES, ano, AutoriaEmMassa.class, registro -> {
                if (registro.deDeputado()) {
                    lote.adicionar(registro);
                }
            });
        }
        return lote.total();
    }

    /**
     * A autoria só é gravada quando as duas pontas já existem no banco, e é isso que o
     * {@code SELECT} da inserção verifica. Assinatura de deputado que não está na base — de
     * legislatura anterior, por exemplo — é ignorada em vez de virar registro órfão.
     */
    private void gravarAutorias(List<AutoriaEmMassa> registros) {
        jdbc.batchUpdate("""
                INSERT INTO proposicao_autor (proposicao_id, parlamentar_id, ordem_assinatura, proponente)
                SELECT pr.id, pa.id, ?, ?
                FROM proposicao pr, parlamentar pa
                WHERE pr.casa = ? AND pr.id_externo = ?
                  AND pa.casa = ? AND pa.id_externo = ?
                ON CONFLICT (proposicao_id, parlamentar_id) DO UPDATE SET
                    ordem_assinatura = EXCLUDED.ordem_assinatura,
                    proponente = EXCLUDED.proponente
                """, registros, registros.size(), (ps, r) -> {
            setInteiro(ps, 1, r.ordem());
            ps.setBoolean(2, r.ehProponente());
            ps.setString(3, CAMARA);
            ps.setString(4, String.valueOf(r.idProposicao()));
            ps.setString(5, CAMARA);
            ps.setString(6, String.valueOf(r.idDeputadoAutor()));
        });
    }

    private long importarTemas(int ano) {
        GravadorEmLote<TemaEmMassa> lote = new GravadorEmLote<>(TAMANHO_DO_LOTE, this::gravarTemas);
        try (lote) {
            arquivos.lerAno(ArquivoEmMassa.PROPOSICOES_TEMAS, ano, TemaEmMassa.class, registro -> {
                if (registro.codTema() != null && registro.idProposicao() != null) {
                    lote.adicionar(registro);
                }
            });
        }
        return lote.total();
    }

    private void gravarTemas(List<TemaEmMassa> registros) {
        jdbc.batchUpdate("""
                INSERT INTO tema (cod_externo, nome) VALUES (?, ?)
                ON CONFLICT (cod_externo) DO UPDATE SET nome = EXCLUDED.nome
                """, registros, registros.size(), (ps, r) -> {
            ps.setInt(1, r.codTema());
            ps.setString(2, r.tema() == null ? "Sem tema" : r.tema());
        });

        jdbc.batchUpdate("""
                INSERT INTO proposicao_tema (proposicao_id, tema_id, relevancia)
                SELECT pr.id, t.id, ?
                FROM proposicao pr, tema t
                WHERE pr.casa = ? AND pr.id_externo = ? AND t.cod_externo = ?
                ON CONFLICT (proposicao_id, tema_id) DO UPDATE SET relevancia = EXCLUDED.relevancia
                """, registros, registros.size(), (ps, r) -> {
            ps.setInt(1, r.relevancia() == null ? 0 : r.relevancia());
            ps.setString(2, CAMARA);
            ps.setString(3, r.idProposicao());
            ps.setInt(4, r.codTema());
        });
    }

    private long importarVotacoes(int ano) {
        GravadorEmLote<VotacaoEmMassa> lote = new GravadorEmLote<>(TAMANHO_DO_LOTE, this::gravarVotacoes);
        try (lote) {
            arquivos.lerAno(ArquivoEmMassa.VOTACOES, ano, VotacaoEmMassa.class, registro -> {
                if (registro.id() != null) {
                    lote.adicionar(registro);
                }
            });
        }
        return lote.total();
    }

    /**
     * {@code nominal} entra como falso e é corrigido pela importação dos votos: aqui ainda não se
     * sabe se houve registro individual, e afirmar que houve seria dizer algo que o arquivo de
     * votações não diz.
     */
    private void gravarVotacoes(List<VotacaoEmMassa> registros) {
        jdbc.batchUpdate("""
                INSERT INTO votacao (casa, id_externo, data, descricao, sigla_orgao, aprovada,
                                     nominal, secreta, votos_sim, votos_nao, votos_outros)
                VALUES (?, ?, ?, ?, ?, ?, FALSE, FALSE, ?, ?, ?)
                ON CONFLICT (casa, id_externo) DO UPDATE SET
                    data = EXCLUDED.data,
                    descricao = EXCLUDED.descricao,
                    sigla_orgao = EXCLUDED.sigla_orgao,
                    aprovada = EXCLUDED.aprovada,
                    votos_sim = EXCLUDED.votos_sim,
                    votos_nao = EXCLUDED.votos_nao,
                    votos_outros = EXCLUDED.votos_outros
                """, registros, registros.size(), (ps, r) -> {
            ps.setString(1, CAMARA);
            ps.setString(2, r.id());
            setData(ps, 3, Datas.paraData(r.data()));
            ps.setString(4, r.descricao());
            ps.setString(5, r.siglaOrgao());
            setBooleano(ps, 6, r.aprovacao() == null ? null : r.aprovacao() == 1);
            setInteiro(ps, 7, r.votosSim());
            setInteiro(ps, 8, r.votosNao());
            setInteiro(ps, 9, r.votosOutros());
        });
    }

    private long importarVinculoVotacaoProposicao(int ano) {
        GravadorEmLote<VotacaoProposicaoEmMassa> lote =
                new GravadorEmLote<>(TAMANHO_DO_LOTE, this::gravarVinculos);
        try (lote) {
            arquivos.lerAno(ArquivoEmMassa.VOTACOES_PROPOSICOES, ano, VotacaoProposicaoEmMassa.class, registro -> {
                if (registro.idVotacao() != null && registro.proposicao() != null) {
                    lote.adicionar(registro);
                }
            });
        }
        return lote.total();
    }

    /**
     * Sem esse vínculo a tela só teria a descrição da deliberação — "Rejeitada a Emenda de
     * Plenário nº 1" — sem dizer de que projeto se trata. O título da proposta é guardado junto
     * porque o arquivo o traz pronto e ele continua útil quando a proposta em si é de um ano que
     * ainda não foi importado.
     */
    private void gravarVinculos(List<VotacaoProposicaoEmMassa> registros) {
        jdbc.batchUpdate("""
                UPDATE votacao SET
                    titulo_proposicao = ?,
                    proposicao_id = COALESCE(
                        (SELECT pr.id FROM proposicao pr WHERE pr.casa = ? AND pr.id_externo = ?),
                        proposicao_id)
                WHERE casa = ? AND id_externo = ?
                """, registros, registros.size(), (ps, r) -> {
            var proposicao = r.proposicao();
            ps.setString(1, proposicao.titulo());
            ps.setString(2, CAMARA);
            ps.setString(3, String.valueOf(proposicao.id()));
            ps.setString(4, CAMARA);
            ps.setString(5, r.idVotacao());
        });
    }

    private long importarVotos(int ano) {
        GravadorEmLote<VotoEmMassa> lote = new GravadorEmLote<>(TAMANHO_DO_LOTE, this::gravarVotos);
        try (lote) {
            arquivos.lerAno(ArquivoEmMassa.VOTACOES_VOTOS, ano, VotoEmMassa.class, registro -> {
                if (registro.idVotacao() != null && registro.deputado() != null
                        && registro.deputado().id() != null) {
                    lote.adicionar(registro);
                }
            });
        }
        return lote.total();
    }

    private void gravarVotos(List<VotoEmMassa> registros) {
        jdbc.batchUpdate("""
                INSERT INTO voto_parlamentar (votacao_id, parlamentar_id, tipo_voto, descricao_origem, data_registro)
                SELECT vt.id, pa.id, ?, ?, ?
                FROM votacao vt, parlamentar pa
                WHERE vt.casa = ? AND vt.id_externo = ?
                  AND pa.casa = ? AND pa.id_externo = ?
                ON CONFLICT (votacao_id, parlamentar_id) DO UPDATE SET
                    tipo_voto = EXCLUDED.tipo_voto,
                    descricao_origem = EXCLUDED.descricao_origem,
                    data_registro = EXCLUDED.data_registro
                """, registros, registros.size(), (ps, r) -> {
            ps.setString(1, TipoVoto.daDescricao(r.voto()).name());
            ps.setString(2, r.voto());
            var instante = Datas.paraInstante(r.dataHoraVoto());
            if (instante == null) {
                ps.setNull(3, Types.TIMESTAMP_WITH_TIMEZONE);
            } else {
                ps.setObject(3, instante.atOffset(java.time.ZoneOffset.UTC));
            }
            ps.setString(4, CAMARA);
            ps.setString(5, r.idVotacao());
            ps.setString(6, CAMARA);
            ps.setString(7, r.deputado().id());
        });

        // Votação que recebeu voto individual é nominal; as demais foram decididas de forma
        // simbólica, e essa diferença é o que a tela usa para não sugerir omissão do parlamentar.
        jdbc.update("""
                UPDATE votacao SET nominal = TRUE
                WHERE nominal = FALSE
                  AND EXISTS (SELECT 1 FROM voto_parlamentar v WHERE v.votacao_id = votacao.id)
                """);
    }

    private static void setInteiro(java.sql.PreparedStatement ps, int posicao, Integer valor)
            throws java.sql.SQLException {
        if (valor == null) {
            ps.setNull(posicao, Types.INTEGER);
        } else {
            ps.setInt(posicao, valor);
        }
    }

    private static void setBooleano(java.sql.PreparedStatement ps, int posicao, Boolean valor)
            throws java.sql.SQLException {
        if (valor == null) {
            ps.setNull(posicao, Types.BOOLEAN);
        } else {
            ps.setBoolean(posicao, valor);
        }
    }

    private static void setData(java.sql.PreparedStatement ps, int posicao, LocalDate valor)
            throws java.sql.SQLException {
        if (valor == null) {
            ps.setNull(posicao, Types.DATE);
        } else {
            ps.setObject(posicao, valor);
        }
    }
}
