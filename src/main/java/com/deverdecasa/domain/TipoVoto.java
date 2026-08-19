package com.deverdecasa.domain;

import java.util.Locale;

/**
 * Sentido do voto de um parlamentar numa votação nominal.
 *
 * <p>Além dos quatro sentidos que interessam ao leitor (sim, não, abstenção e ausência), as duas
 * casas registram situações que não são voto e que seriam distorcidas se fossem achatadas em
 * "ausente": obstrução, a presidência que só vota para desempatar e o voto oculto de uma sessão
 * secreta. Cada uma ganha seu próprio valor, e o texto original da fonte é preservado ao lado
 * em {@code VotoParlamentar#descricaoOrigem}.
 */
public enum TipoVoto {
    SIM,
    NAO,
    ABSTENCAO,
    AUSENTE,
    OBSTRUCAO,
    /** Presidente da sessão: art. 17 do Regimento da Câmara, art. 51 do Regimento do Senado. */
    PRESIDENTE,
    /** Estava presente e não registrou voto (P-NRV, no vocabulário do Senado). */
    NAO_REGISTROU,
    /** Votação secreta: o registro existe, mas o sentido do voto não é público. */
    SECRETO,
    /** Sentido não reconhecido; o texto da fonte fica guardado para não se perder o dado. */
    OUTRO;

    /** Traduz o rótulo cru da Câmara ({@code tipoVoto}) ou do Senado ({@code siglaVotoParlamentar}). */
    public static TipoVoto daDescricao(String descricao) {
        if (descricao == null || descricao.isBlank()) {
            return OUTRO;
        }
        String normalizado = descricao.trim().toLowerCase(Locale.ROOT);
        return switch (normalizado) {
            case "sim" -> SIM;
            case "não", "nao" -> NAO;
            case "abstenção", "abstencao" -> ABSTENCAO;
            case "obstrução", "obstrucao" -> OBSTRUCAO;
            case "votou" -> SECRETO;
            case "p-nrv" -> NAO_REGISTROU;
            // Ausências do Senado: não compareceu, atividade parlamentar, missão e licenças.
            case "ncom", "ap", "lap", "mis", "lp", "ls", "na" -> AUSENTE;
            default -> normalizado.startsWith("artigo 17") || normalizado.startsWith("presidente")
                    ? PRESIDENTE
                    : OUTRO;
        };
    }
}
