package com.deverdecasa.mapper;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

/**
 * Decide se uma proposição chegou a ser aprovada, a partir do texto de situação que a casa
 * publica.
 *
 * <p>A leitura é deliberadamente conservadora: só afirma aprovação quando a situação diz que a
 * proposta venceu uma etapa decisiva — virou norma, seguiu para a outra casa, espera sanção ou
 * autógrafos. Tudo o mais que a casa informa (em relatoria, pronta para pauta, tramitando em
 * conjunto, arquivada, retirada pelo autor) conta como não aprovada, e situação ausente fica
 * como desconhecida em vez de virar uma negativa que a fonte não deu.
 *
 * <p>O texto original continua guardado em {@code Proposicao#situacao} e é exibido ao lado da
 * classificação, para que o leitor confira a leitura em vez de ter que confiar nela.
 */
public final class SituacaoProposicao {

    private static final List<String> APROVADAS = List.of(
            "transformado em norma juridica",
            "transformada em norma juridica",
            "aguardando apreciacao pelo senado federal",
            "aguardando sancao",
            "aguardando autografos",
            "remetida ao senado federal",
            "aprovada",
            "aprovado");

    private SituacaoProposicao() {
    }

    /** {@code null} quando a casa não informou situação: desconhecido não é reprovado. */
    public static Boolean aprovada(String descricaoSituacao) {
        if (descricaoSituacao == null || descricaoSituacao.isBlank()) {
            return null;
        }
        String normalizada = semAcento(descricaoSituacao);
        return APROVADAS.stream().anyMatch(normalizada::startsWith);
    }

    private static String semAcento(String texto) {
        return Normalizer.normalize(texto.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }
}
