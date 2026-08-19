package com.deverdecasa.service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Acumula registros e descarrega de tempos em tempos.
 *
 * <p>A carga em massa processa dezenas de milhares de linhas por arquivo. Gravar uma a uma
 * transformaria a importação em dezenas de milhares de idas ao banco; guardar tudo para o fim
 * exigiria manter o arquivo inteiro em memória, que é justamente o que a leitura incremental
 * evita. O lote fica no meio: memória limitada ao seu tamanho e uma ida ao banco por lote.
 */
class GravadorEmLote<T> implements AutoCloseable {

    private final int tamanhoDoLote;
    private final Consumer<List<T>> descarga;
    private final List<T> pendentes;
    private long total;

    GravadorEmLote(int tamanhoDoLote, Consumer<List<T>> descarga) {
        this.tamanhoDoLote = tamanhoDoLote;
        this.descarga = descarga;
        this.pendentes = new ArrayList<>(tamanhoDoLote);
    }

    void adicionar(T registro) {
        pendentes.add(registro);
        if (pendentes.size() >= tamanhoDoLote) {
            descarregar();
        }
    }

    long total() {
        return total;
    }

    private void descarregar() {
        if (pendentes.isEmpty()) {
            return;
        }
        descarga.accept(List.copyOf(pendentes));
        total += pendentes.size();
        pendentes.clear();
    }

    @Override
    public void close() {
        descarregar();
    }
}
