package com.deverdecasa.repository;

import com.deverdecasa.domain.Casa;
import com.deverdecasa.domain.Proposicao;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProposicaoRepository extends JpaRepository<Proposicao, Long> {

    Optional<Proposicao> findByCasaAndIdExterno(Casa casa, String idExterno);
}
