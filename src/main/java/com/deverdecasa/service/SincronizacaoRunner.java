package com.deverdecasa.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.deverdecasa.integracao.camara.CamaraApiProperties;
import java.time.Year;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Dispara as duas formas de carga sem esperar o agendamento.
 *
 * <p>{@code --importar-massa} faz a carga inicial pelos arquivos publicados em bloco, que é o
 * caminho para encher a base de uma vez. {@code --sincronizar} roda a atualização incremental
 * pela API, que é o movimento do dia a dia.
 *
 * <p>Fica como argumento de execução em vez de endpoint HTTP de propósito: disparar uma varredura
 * das APIs públicas é operação de manutenção, não algo que deva ficar exposto na web sem controle
 * de acesso.
 */
@Component
public class SincronizacaoRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SincronizacaoRunner.class);

    private final SincronizacaoService sincronizacao;
    private final ImportacaoMassaCamaraService importacaoEmMassa;
    private final SincronizacaoProperties properties;
    private final CamaraApiProperties camara;

    public SincronizacaoRunner(SincronizacaoService sincronizacao,
                               ImportacaoMassaCamaraService importacaoEmMassa,
                               SincronizacaoProperties properties,
                               CamaraApiProperties camara) {
        this.sincronizacao = sincronizacao;
        this.importacaoEmMassa = importacaoEmMassa;
        this.properties = properties;
        this.camara = camara;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (args.containsOption("importar-massa")) {
            importarEmMassa();
        }
        if (properties.aoIniciar() || args.containsOption("sincronizar")) {
            log.info("Sincronização solicitada na inicialização");
            sincronizacao.sincronizarTudo();
        }
    }

    /**
     * Os parlamentares vêm antes dos arquivos: autorias e votos só são gravados quando as duas
     * pontas existem, e sem eles a carga entraria pela metade.
     */
    private void importarEmMassa() {
        log.info("Carga inicial a partir dos arquivos de dados abertos");
        sincronizacao.sincronizarParlamentares();

        int anoAtual = Year.now().getValue();
        ResultadoSincronizacao total = ResultadoSincronizacao.vazio();
        for (int i = 0; i < camara.anosDeImportacao(); i++) {
            total = total.mais(importacaoEmMassa.importarAno(anoAtual - i));
        }
        log.info("Carga inicial concluída: {}", total);
    }
}
