package com.deverdecasa.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.time.LocalDate;

/** Projeto, requerimento ou emenda apresentado numa das casas. */
@Entity
@Table(name = "proposicao",
        uniqueConstraints = @UniqueConstraint(name = "uk_proposicao_casa_id_externo",
                columnNames = {"casa", "id_externo"}))
public class Proposicao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Casa casa;

    @Column(name = "id_externo", nullable = false, length = 40)
    private String idExterno;

    /** PL, PEC, PLP, RQS: como a casa classifica a proposta. */
    @Column(name = "sigla_tipo", nullable = false, length = 20)
    private String siglaTipo;

    private Integer numero;

    private Integer ano;

    @Column(columnDefinition = "text")
    private String ementa;

    @Column(name = "data_apresentacao")
    private LocalDate dataApresentacao;

    /** Texto de situação como a casa publica, preservado sem interpretação. */
    @Column(length = 300)
    private String situacao;

    /**
     * Nulo enquanto a situação não foi consultada: o desconhecido não vira "não aprovada",
     * senão o filtro passaria a afirmar algo que a fonte não disse.
     */
    private Boolean aprovada;

    @Column(name = "url_inteiro_teor", length = 500)
    private String urlInteiroTeor;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm = Instant.now();

    protected Proposicao() {
    }

    public Proposicao(Casa casa, String idExterno, String siglaTipo) {
        this.casa = casa;
        this.idExterno = idExterno;
        this.siglaTipo = siglaTipo;
    }

    /** Como a proposta é citada publicamente, por exemplo "PL 4916/2026". */
    public String getIdentificacao() {
        StringBuilder sb = new StringBuilder(siglaTipo);
        if (numero != null) {
            sb.append(' ').append(numero);
        }
        if (ano != null) {
            sb.append('/').append(ano);
        }
        return sb.toString();
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

    public String getSiglaTipo() {
        return siglaTipo;
    }

    public void setSiglaTipo(String siglaTipo) {
        this.siglaTipo = siglaTipo;
    }

    public Integer getNumero() {
        return numero;
    }

    public void setNumero(Integer numero) {
        this.numero = numero;
    }

    public Integer getAno() {
        return ano;
    }

    public void setAno(Integer ano) {
        this.ano = ano;
    }

    public String getEmenta() {
        return ementa;
    }

    public void setEmenta(String ementa) {
        this.ementa = ementa;
    }

    public LocalDate getDataApresentacao() {
        return dataApresentacao;
    }

    public void setDataApresentacao(LocalDate dataApresentacao) {
        this.dataApresentacao = dataApresentacao;
    }

    public String getSituacao() {
        return situacao;
    }

    public void setSituacao(String situacao) {
        this.situacao = situacao;
    }

    public Boolean getAprovada() {
        return aprovada;
    }

    public void setAprovada(Boolean aprovada) {
        this.aprovada = aprovada;
    }

    public String getUrlInteiroTeor() {
        return urlInteiroTeor;
    }

    public void setUrlInteiroTeor(String urlInteiroTeor) {
        this.urlInteiroTeor = urlInteiroTeor;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }

    public void setAtualizadoEm(Instant atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
    }
}
