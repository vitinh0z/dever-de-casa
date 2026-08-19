package com.deverdecasa.repository;

import com.deverdecasa.domain.Partido;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PartidoRepository extends JpaRepository<Partido, Long> {

    Optional<Partido> findBySigla(String sigla);

    /** Siglas que de fato têm parlamentar associado, para não oferecer filtro que não filtra nada. */
    @Query("""
            SELECT DISTINCT p.partido.sigla FROM Parlamentar p
            WHERE p.partido IS NOT NULL
            ORDER BY p.partido.sigla
            """)
    List<String> siglasComParlamentar();
}
