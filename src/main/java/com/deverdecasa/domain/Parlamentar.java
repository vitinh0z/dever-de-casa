package com.deverdecasa.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

/**
 * Deputado federal ou senador. A identidade vem do par casa + identificador de origem, já que os
 * códigos da Câmara e do Senado são numerações independentes e podem colidir.
 */
@Entity
@Table(name = "parlamentar",
        uniqueConstraints = @UniqueConstraint(name = "uk_parlamentar_casa_id_externo",
                columnNames = {"casa", "id_externo"}))
public class Parlamentar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Casa casa;

    @Column(name = "id_externo", nullable = false, length = 40)
    private String idExterno;

    /** Nome com que o parlamentar assina o mandato, que é como o cidadão vai procurar. */
    @Column(nullable = false, length = 200)
    private String nome;

    @Column(name = "nome_civil", length = 200)
    private String nomeCivil;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "partido_id", foreignKey = @jakarta.persistence.ForeignKey(name = "fk_parlamentar_partido"))
    private Partido partido;

    @Column(name = "sigla_uf", length = 2)
    private String siglaUf;

    @Column(name = "url_foto", length = 500)
    private String urlFoto;

    @Column(length = 200)
    private String email;

    /** Situação do mandato conforme a casa: "Exercício", "Afastado", "Licenciado". */
    @Column(length = 100)
    private String situacao;

    @Column(name = "condicao_eleitoral", length = 100)
    private String condicaoEleitoral;

    @Column(name = "id_legislatura")
    private Integer idLegislatura;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm = Instant.now();

    protected Parlamentar() {
    }

    public Parlamentar(Casa casa, String idExterno, String nome) {
        this.casa = casa;
        this.idExterno = idExterno;
        this.nome = nome;
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

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getNomeCivil() {
        return nomeCivil;
    }

    public void setNomeCivil(String nomeCivil) {
        this.nomeCivil = nomeCivil;
    }

    public Partido getPartido() {
        return partido;
    }

    public void setPartido(Partido partido) {
        this.partido = partido;
    }

    public String getSiglaUf() {
        return siglaUf;
    }

    public void setSiglaUf(String siglaUf) {
        this.siglaUf = siglaUf;
    }

    public String getUrlFoto() {
        return urlFoto;
    }

    public void setUrlFoto(String urlFoto) {
        this.urlFoto = urlFoto;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSituacao() {
        return situacao;
    }

    public void setSituacao(String situacao) {
        this.situacao = situacao;
    }

    public String getCondicaoEleitoral() {
        return condicaoEleitoral;
    }

    public void setCondicaoEleitoral(String condicaoEleitoral) {
        this.condicaoEleitoral = condicaoEleitoral;
    }

    public Integer getIdLegislatura() {
        return idLegislatura;
    }

    public void setIdLegislatura(Integer idLegislatura) {
        this.idLegislatura = idLegislatura;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }

    public void setAtualizadoEm(Instant atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
    }
}
