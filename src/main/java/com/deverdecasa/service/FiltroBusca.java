package com.deverdecasa.service;

import com.deverdecasa.domain.Casa;

/**
 * Critérios da tela de busca, já normalizados.
 *
 * @param termo     trecho do nome digitado; nulo quando o visitante não digitou nada
 * @param partido   sigla do partido
 * @param casa      restringe a uma das casas legislativas
 * @param aprovadas quando preenchido, a contagem de proposições exibida passa a considerar só as
 *                  aprovadas ({@code true}) ou só as que não foram aprovadas ({@code false})
 */
public record FiltroBusca(String termo, String partido, Casa casa, Boolean aprovadas) {

    /** Aceita o que vem da query string e devolve campos vazios como nulos. */
    public static FiltroBusca de(String termo, String partido, String casa, Boolean aprovadas) {
        return new FiltroBusca(limpar(termo), limpar(partido), casaDe(casa), aprovadas);
    }

    public boolean vazio() {
        return termo == null && partido == null && casa == null && aprovadas == null;
    }

    public String casaComoTexto() {
        return casa == null ? null : casa.name();
    }

    private static String limpar(String valor) {
        if (valor == null) {
            return null;
        }
        String limpo = valor.trim();
        return limpo.isEmpty() ? null : limpo;
    }

    private static Casa casaDe(String valor) {
        String limpo = limpar(valor);
        if (limpo == null) {
            return null;
        }
        try {
            return Casa.valueOf(limpo.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            // Valor fora do vocabulário chega pela URL; ignorar o filtro é melhor que estourar
            // uma página de erro por causa de um parâmetro digitado à mão.
            return null;
        }
    }
}
