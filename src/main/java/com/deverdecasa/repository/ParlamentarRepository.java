package com.deverdecasa.repository;

import com.deverdecasa.domain.Casa;
import com.deverdecasa.domain.Parlamentar;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ParlamentarRepository extends JpaRepository<Parlamentar, Long> {

    Optional<Parlamentar> findByCasaAndIdExterno(Casa casa, String idExterno);

    /**
     * Busca por nome com os filtros da tela de listagem combinados entre si.
     *
     * <p>A condição de nome usa duas travessias que o índice GIN trigram cobre: o LIKE de
     * substring, que atende quem digitou poucas letras, e o operador {@code %} de similaridade,
     * que atende quem digitou o nome com erro. {@code similarity()} sozinho não usaria o índice,
     * por isso ele aparece apenas na ordenação.
     *
     * <p>A contagem de proposições respeita o filtro de aprovação: com {@code :aprovada}
     * preenchido, quem não tem nenhuma proposição naquele estado aparece com zero em vez de
     * sumir da lista, porque a pergunta da tela é sobre o parlamentar, não sobre a proposição.
     *
     * <p>A contagem sai de uma subconsulta por parlamentar, e não de um agrupamento sobre a
     * junção inteira. Agrupando, o banco percorria as centenas de milhares de autorias da base
     * para exibir vinte linhas, e a listagem sem filtro levava segundos; por parlamentar, o
     * trabalho fica limitado à página pedida e ao índice de autoria.
     *
     * <p>A proposição só é consultada quando há filtro de aprovação, e por {@code EXISTS} em vez
     * de junção. Sem filtro — o caso da primeira visita — a contagem se resolve só pelo índice de
     * autoria, e a listagem responde em milissegundos em lugar de segundos. A comparação usa
     * igualdade, e não {@code IS NOT DISTINCT FROM}, porque esta última impede o índice de
     * aprovação de ser usado; a diferença entre as duas não importa aqui, já que filtro ausente
     * chega como nulo e nem entra nessa condição.
     *
     * <p>Proposição de situação desconhecida fica de fora dos dois filtros, e é o correto: ela não
     * foi aprovada nem deixou de ser, a casa apenas não informou em que pé está.
     */
    @Query(value = """
            SELECT p.id       AS id,
                   p.nome     AS nome,
                   p.casa     AS casa,
                   p.sigla_uf AS siglaUf,
                   pt.sigla   AS siglaPartido,
                   p.url_foto AS urlFoto,
                   (SELECT COUNT(*)
                      FROM proposicao_autor pa
                     WHERE pa.parlamentar_id = p.id
                       AND (CAST(:aprovada AS BOOLEAN) IS NULL
                            OR EXISTS (SELECT 1 FROM proposicao pr
                                        WHERE pr.id = pa.proposicao_id
                                          AND pr.aprovada = CAST(:aprovada AS BOOLEAN)))
                   ) AS qtdProposicoes
            FROM parlamentar p
                     LEFT JOIN partido pt ON pt.id = p.partido_id
            WHERE (CAST(:termo AS TEXT) IS NULL
                       OR normaliza_nome(p.nome) LIKE '%' || normaliza_nome(CAST(:termo AS TEXT)) || '%'
                       OR normaliza_nome(p.nome) % normaliza_nome(CAST(:termo AS TEXT)))
              AND (CAST(:partido AS TEXT) IS NULL OR pt.sigla = CAST(:partido AS TEXT))
              AND (CAST(:casa AS TEXT) IS NULL OR p.casa = CAST(:casa AS TEXT))
            ORDER BY CASE WHEN CAST(:termo AS TEXT) IS NULL THEN 0
                          ELSE similarity(normaliza_nome(p.nome), normaliza_nome(CAST(:termo AS TEXT))) END DESC,
                     p.nome
            """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM parlamentar p
                             LEFT JOIN partido pt ON pt.id = p.partido_id
                    WHERE (CAST(:termo AS TEXT) IS NULL
                               OR normaliza_nome(p.nome) LIKE '%' || normaliza_nome(CAST(:termo AS TEXT)) || '%'
                               OR normaliza_nome(p.nome) % normaliza_nome(CAST(:termo AS TEXT)))
                      AND (CAST(:partido AS TEXT) IS NULL OR pt.sigla = CAST(:partido AS TEXT))
                      AND (CAST(:casa AS TEXT) IS NULL OR p.casa = CAST(:casa AS TEXT))
                    """,
            nativeQuery = true)
    Page<ParlamentarResumo> buscar(@Param("termo") String termo,
                                   @Param("partido") String partido,
                                   @Param("casa") String casa,
                                   @Param("aprovada") Boolean aprovada,
                                   Pageable pageable);
}
