package com.deverdecasa.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Liga um parlamentar a uma proposição que ele assinou. É o vínculo que separa "autorou" de
 * "discursou sobre", que é justamente o que o projeto se propõe a mostrar.
 */
@Entity
@Table(name = "proposicao_autor",
        uniqueConstraints = @UniqueConstraint(name = "uk_proposicao_autor",
                columnNames = {"proposicao_id", "parlamentar_id"}))
public class ProposicaoAutor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proposicao_id", foreignKey = @ForeignKey(name = "fk_proposicao_autor_proposicao"))
    private Proposicao proposicao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parlamentar_id", foreignKey = @ForeignKey(name = "fk_proposicao_autor_parlamentar"))
    private Parlamentar parlamentar;

    @Column(name = "ordem_assinatura")
    private Integer ordemAssinatura;

    /** Autor principal, em oposição aos coautores que apenas assinam junto. */
    @Column(nullable = false)
    private boolean proponente;

    protected ProposicaoAutor() {
    }

    public ProposicaoAutor(Proposicao proposicao, Parlamentar parlamentar, boolean proponente) {
        this.proposicao = proposicao;
        this.parlamentar = parlamentar;
        this.proponente = proponente;
    }

    public Long getId() {
        return id;
    }

    public Proposicao getProposicao() {
        return proposicao;
    }

    public void setProposicao(Proposicao proposicao) {
        this.proposicao = proposicao;
    }

    public Parlamentar getParlamentar() {
        return parlamentar;
    }

    public void setParlamentar(Parlamentar parlamentar) {
        this.parlamentar = parlamentar;
    }

    public Integer getOrdemAssinatura() {
        return ordemAssinatura;
    }

    public void setOrdemAssinatura(Integer ordemAssinatura) {
        this.ordemAssinatura = ordemAssinatura;
    }

    public boolean isProponente() {
        return proponente;
    }

    public void setProponente(boolean proponente) {
        this.proponente = proponente;
    }
}
