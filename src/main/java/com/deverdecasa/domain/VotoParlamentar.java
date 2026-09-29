package com.deverdecasa.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

/** Como um parlamentar votou numa votação nominal. */
@Entity
@Table(name = "voto_parlamentar",
        uniqueConstraints = @UniqueConstraint(name = "uk_voto_parlamentar",
                columnNames = {"votacao_id", "parlamentar_id"}))
public class VotoParlamentar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "votacao_id", foreignKey = @ForeignKey(name = "fk_voto_votacao"))
    private Votacao votacao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parlamentar_id", foreignKey = @ForeignKey(name = "fk_voto_parlamentar"))
    private Parlamentar parlamentar;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_voto", nullable = false, length = 20)
    private TipoVoto tipoVoto;

    /** Rótulo exato publicado pela casa, guardado para não achatar o que a fonte disse. */
    @Column(name = "descricao_origem", length = 100)
    private String descricaoOrigem;

    @Column(name = "data_registro")
    private Instant dataRegistro;

    protected VotoParlamentar() {
    }

    public VotoParlamentar(Votacao votacao, Parlamentar parlamentar, TipoVoto tipoVoto) {
        this.votacao = votacao;
        this.parlamentar = parlamentar;
        this.tipoVoto = tipoVoto;
    }

    public Long getId() {
        return id;
    }

    public Votacao getVotacao() {
        return votacao;
    }

    public void setVotacao(Votacao votacao) {
        this.votacao = votacao;
    }

    public Parlamentar getParlamentar() {
        return parlamentar;
    }

    public void setParlamentar(Parlamentar parlamentar) {
        this.parlamentar = parlamentar;
    }

    public TipoVoto getTipoVoto() {
        return tipoVoto;
    }

    public void setTipoVoto(TipoVoto tipoVoto) {
        this.tipoVoto = tipoVoto;
    }

    public String getDescricaoOrigem() {
        return descricaoOrigem;
    }

    public void setDescricaoOrigem(String descricaoOrigem) {
        this.descricaoOrigem = descricaoOrigem;
    }

    public Instant getDataRegistro() {
        return dataRegistro;
    }

    public void setDataRegistro(Instant dataRegistro) {
        this.dataRegistro = dataRegistro;
    }
}
