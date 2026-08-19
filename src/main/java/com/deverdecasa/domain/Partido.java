package com.deverdecasa.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Legenda partidária, identificada pela sigla com que aparece nas duas casas. */
@Entity
@Table(name = "partido")
public class Partido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String sigla;

    /** A Câmara publica o nome por extenso; o Senado só devolve a sigla. */
    @Column(length = 200)
    private String nome;

    protected Partido() {
    }

    public Partido(String sigla) {
        this.sigla = sigla;
    }

    public Long getId() {
        return id;
    }

    public String getSigla() {
        return sigla;
    }

    public void setSigla(String sigla) {
        this.sigla = sigla;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }
}
