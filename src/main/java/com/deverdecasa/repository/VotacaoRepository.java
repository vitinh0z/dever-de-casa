package com.deverdecasa.repository;

import com.deverdecasa.domain.Casa;
import com.deverdecasa.domain.Votacao;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VotacaoRepository extends JpaRepository<Votacao, Long> {

    Optional<Votacao> findByCasaAndIdExterno(Casa casa, String idExterno);

    /**
     * Votações sem voto nominal que decidiram proposições do parlamentar. Sustentam o aviso do
     * perfil: ali a casa deliberou de forma simbólica e ninguém teve voto registrado, o que é
     * diferente de o parlamentar ter faltado.
     */
    @Query("""
            SELECT DISTINCT vt FROM Votacao vt
            JOIN FETCH vt.proposicao pr
            WHERE vt.nominal = FALSE
              AND pr.id IN (SELECT pa.proposicao.id FROM ProposicaoAutor pa WHERE pa.parlamentar.id = :parlamentarId)
            ORDER BY vt.data DESC NULLS LAST
            """)
    List<Votacao> simbolicasDasProposicoesDoParlamentar(@Param("parlamentarId") Long parlamentarId,
                                                       Pageable pageable);
}
