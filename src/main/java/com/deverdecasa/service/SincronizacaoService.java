package com.deverdecasa.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Ponto único de disparo da sincronização, agregando as casas legislativas cobertas.
 *
 * <p>Cada casa roda de forma independente: se a API de uma estiver fora do ar, a outra ainda
 * atualiza o que consegue.
 */
@Service
public class SincronizacaoService {

    private static final Logger log = LoggerFactory.getLogger(SincronizacaoService.class);

    private final SincronizacaoCamaraService camara;
    private final SincronizacaoProperties properties;

    public SincronizacaoService(SincronizacaoCamaraService camara, SincronizacaoProperties properties) {
        this.camara = camara;
        this.properties = properties;
    }

    public ResultadoSincronizacao sincronizarTudo() {
        if (!properties.habilitada()) {
            log.info("Sincronização desligada por configuração; nada a fazer.");
            return ResultadoSincronizacao.vazio();
        }
        long inicio = System.currentTimeMillis();
        ResultadoSincronizacao resultado = executar("Câmara", camara::sincronizar);
        log.info("Sincronização concluída em {}s: {}", (System.currentTimeMillis() - inicio) / 1000, resultado);
        return resultado;
    }

    private ResultadoSincronizacao executar(String casa, java.util.function.Supplier<ResultadoSincronizacao> tarefa) {
        try {
            return tarefa.get();
        } catch (RuntimeException e) {
            log.error("Sincronização da {} falhou por completo: {}", casa, e.toString());
            return new ResultadoSincronizacao(0, 0, 0, 0, 1);
        }
    }
}
