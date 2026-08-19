package com.deverdecasa.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Assunto de que uma proposição trata, na classificação da própria casa legislativa.
 *
 * <p>A Câmara mantém uma lista de referência com pouco mais de trinta temas e indexa cada
 * proposta neles. O projeto adota essa classificação em vez de criar categorias próprias:
 * inventar uma taxonomia significaria interpretar o conteúdo das propostas, que é exatamente o
 * tipo de leitura que este projeto não se propõe a fazer.
 */
@Entity
@Table(name = "tema")
public class Tema {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cod_externo", nullable = false, unique = true)
    private Integer codExterno;

    @Column(nullable = false, length = 200)
    private String nome;

    protected Tema() {
    }

    public Tema(Integer codExterno, String nome) {
        this.codExterno = codExterno;
        this.nome = nome;
    }

    public Long getId() {
        return id;
    }

    public Integer getCodExterno() {
        return codExterno;
    }

    public void setCodExterno(Integer codExterno) {
        this.codExterno = codExterno;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }
}
