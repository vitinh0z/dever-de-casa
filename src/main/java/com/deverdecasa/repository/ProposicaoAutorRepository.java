package com.deverdecasa.repository;

import com.deverdecasa.domain.Parlamentar;
import com.deverdecasa.domain.Proposicao;
import com.deverdecasa.domain.ProposicaoAutor;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProposicaoAutorRepository extends JpaRepository<ProposicaoAutor, Long> {

    Optional<ProposicaoAutor> findByProposicaoAndParlamentar(Proposicao proposicao, Parlamentar parlamentar);

    /**
     * Proposições que o parlamentar assinou, das mais recentes para as mais antigas. O fetch da
     * proposição é obrigatório porque {@code open-in-view} está desligado e o template não teria
     * sessão aberta para resolver o lazy.
     */
    @Query("""
            SELECT pa FROM ProposicaoAutor pa
            JOIN FETCH pa.proposicao pr
            WHERE pa.parlamentar.id = :parlamentarId
            ORDER BY pr.dataApresentacao DESC NULLS LAST, pr.id DESC
            """)
    List<ProposicaoAutor> doParlamentar(@Param("parlamentarId") Long parlamentarId);

    long countByParlamentarId(Long parlamentarId);
}
