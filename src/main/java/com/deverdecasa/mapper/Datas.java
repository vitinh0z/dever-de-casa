package com.deverdecasa.mapper;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

/**
 * Conversão das datas como as duas casas publicam.
 *
 * <p>A Câmara mistura {@code 2026-08-06T13:17} (sem segundos) com {@code 2026-08-13}, e o Senado
 * usa só o formato de data. Nada disso traz fuso, e ambos os registros são de Brasília, então é
 * esse o fuso aplicado ao converter para instante. Texto que não casa com nenhum dos formatos
 * vira nulo: uma data inventada seria pior que a ausência dela.
 */
public final class Datas {

    private static final ZoneId BRASILIA = ZoneId.of("America/Sao_Paulo");

    private Datas() {
    }

    public static LocalDate paraData(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String texto = valor.trim();
        try {
            return texto.length() > 10 ? LocalDateTime.parse(texto).toLocalDate() : LocalDate.parse(texto);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    public static Instant paraInstante(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String texto = valor.trim();
        try {
            if (texto.length() > 10) {
                return LocalDateTime.parse(texto).atZone(BRASILIA).toInstant();
            }
            return LocalDate.parse(texto).atStartOfDay(BRASILIA).toInstant();
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
