package com.deverdecasa.repository;

import com.deverdecasa.domain.Parlamentar;
import com.deverdecasa.domain.Votacao;
import com.deverdecasa.domain.VotoParlamentar;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VotoParlamentarRepository extends JpaRepository<VotoParlamentar, Long> {

    Optional<VotoParlamentar> findByVotacaoAndParlamentar(Votacao votacao, Parlamentar parlamentar);

    /** Histórico de votos nominais, do mais recente para o mais antigo. */
    @Query("""
            SELECT v FROM VotoParlamentar v
            JOIN FETCH v.votacao vt
            LEFT JOIN FETCH vt.proposicao
            WHERE v.parlamentar.id = :parlamentarId
            ORDER BY vt.data DESC NULLS LAST, vt.id DESC
            """)
    List<VotoParlamentar> historicoDoParlamentar(@Param("parlamentarId") Long parlamentarId, Pageable pageable);
}
