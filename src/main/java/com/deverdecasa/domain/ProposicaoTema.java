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

/** Assunto atribuído a uma proposição, com a relevância que a casa deu a ele. */
@Entity
@Table(name = "proposicao_tema",
        uniqueConstraints = @UniqueConstraint(name = "uk_proposicao_tema",
                columnNames = {"proposicao_id", "tema_id"}))
public class ProposicaoTema {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proposicao_id", foreignKey = @ForeignKey(name = "fk_proposicao_tema_proposicao"))
    private Proposicao proposicao;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "tema_id", foreignKey = @ForeignKey(name = "fk_proposicao_tema_tema"))
    private Tema tema;

    /** Distingue o assunto central da proposta dos assuntos apenas tangenciados. */
    @Column(nullable = false)
    private Integer relevancia = 0;

    protected ProposicaoTema() {
    }

    public ProposicaoTema(Proposicao proposicao, Tema tema, Integer relevancia) {
        this.proposicao = proposicao;
        this.tema = tema;
        this.relevancia = relevancia == null ? 0 : relevancia;
    }

    public Long getId() {
        return id;
    }

    public Proposicao getProposicao() {
        return proposicao;
    }

    public Tema getTema() {
        return tema;
    }

    public Integer getRelevancia() {
        return relevancia;
    }

    public void setRelevancia(Integer relevancia) {
        this.relevancia = relevancia;
    }
}
