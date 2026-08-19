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
import java.time.LocalDate;

/**
 * Uma deliberação ocorrida no plenário ou numa comissão.
 *
 * <p>Nem toda votação registra o voto de cada parlamentar: quando é simbólica, a casa apura só o
 * resultado coletivo. {@code nominal} distingue esse caso de uma votação cujos votos ainda não
 * foram sincronizados, para que a tela não sugira omissão onde nunca houve registro individual.
 */
@Entity
@Table(name = "votacao",
        uniqueConstraints = @UniqueConstraint(name = "uk_votacao_casa_id_externo",
                columnNames = {"casa", "id_externo"}))
public class Votacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Casa casa;

    @Column(name = "id_externo", nullable = false, length = 60)
    private String idExterno;

    private LocalDate data;

    @Column(columnDefinition = "text")
    private String descricao;

    @Column(name = "sigla_orgao", length = 40)
    private String siglaOrgao;

    /** Resultado da deliberação; nulo quando a casa não informa. */
    private Boolean aprovada;

    /** Houve registro individual de voto. */
    @Column(nullable = false)
    private boolean nominal;

    /** Sessão secreta: há registro de participação, mas o sentido do voto não é público. */
    @Column(nullable = false)
    private boolean secreta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposicao_id", foreignKey = @ForeignKey(name = "fk_votacao_proposicao"))
    private Proposicao proposicao;

    protected Votacao() {
    }

    public Votacao(Casa casa, String idExterno) {
        this.casa = casa;
        this.idExterno = idExterno;
    }

    public Long getId() {
        return id;
    }

    public Casa getCasa() {
        return casa;
    }

    public void setCasa(Casa casa) {
        this.casa = casa;
    }

    public String getIdExterno() {
        return idExterno;
    }

    public void setIdExterno(String idExterno) {
        this.idExterno = idExterno;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getSiglaOrgao() {
        return siglaOrgao;
    }

    public void setSiglaOrgao(String siglaOrgao) {
        this.siglaOrgao = siglaOrgao;
    }

    public Boolean getAprovada() {
        return aprovada;
    }

    public void setAprovada(Boolean aprovada) {
        this.aprovada = aprovada;
    }

    public boolean isNominal() {
        return nominal;
    }

    public void setNominal(boolean nominal) {
        this.nominal = nominal;
    }

    public boolean isSecreta() {
        return secreta;
    }

    public void setSecreta(boolean secreta) {
        this.secreta = secreta;
    }

    public Proposicao getProposicao() {
        return proposicao;
    }

    public void setProposicao(Proposicao proposicao) {
        this.proposicao = proposicao;
    }
}
