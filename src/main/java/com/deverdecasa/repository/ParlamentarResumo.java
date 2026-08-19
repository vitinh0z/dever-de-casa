package com.deverdecasa.repository;

/** Projeção da listagem: o suficiente para o cartão de resultado, sem carregar a entidade. */
public interface ParlamentarResumo {

    Long getId();

    String getNome();

    String getCasa();

    String getSiglaUf();

    String getSiglaPartido();

    String getUrlFoto();

    /** Quantas proposições o parlamentar assinou dentro do filtro de aprovação aplicado. */
    long getQtdProposicoes();
}
