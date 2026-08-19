package com.deverdecasa.service;

import com.deverdecasa.domain.Casa;
import com.deverdecasa.domain.Parlamentar;
import com.deverdecasa.domain.Partido;
import com.deverdecasa.domain.Proposicao;
import com.deverdecasa.domain.ProposicaoAutor;
import com.deverdecasa.domain.Votacao;
import com.deverdecasa.domain.VotoParlamentar;
import com.deverdecasa.integracao.camara.CamaraApiClient;
import com.deverdecasa.integracao.camara.CamaraDtos.DeputadoApiResponse;
import com.deverdecasa.integracao.camara.CamaraDtos.ProposicaoApiResponse;
import com.deverdecasa.integracao.camara.CamaraDtos.VotacaoApiResponse;
import com.deverdecasa.integracao.camara.CamaraDtos.VotoApiResponse;
import com.deverdecasa.mapper.Datas;
import com.deverdecasa.mapper.ParlamentarMapper;
import com.deverdecasa.mapper.ProposicaoMapper;
import com.deverdecasa.repository.ParlamentarRepository;
import com.deverdecasa.repository.PartidoRepository;
import com.deverdecasa.repository.ProposicaoAutorRepository;
import com.deverdecasa.repository.ProposicaoRepository;
import com.deverdecasa.repository.VotacaoRepository;
import com.deverdecasa.repository.VotoParlamentarRepository;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Traz para o banco local o que a Câmara publica: quem são os deputados, o que assinaram e como
 * votaram.
 *
 * <p>Cada parlamentar e cada votação são gravados em transação própria, aberta pelo
 * {@link TransactionTemplate}. Uma casa legislativa inteira numa transação só significaria perder
 * a execução completa por causa de um registro estranho no meio do caminho; assim, uma falha
 * isolada é contada e o restante segue. O template entra no lugar de {@code @Transactional}
 * porque estes métodos são chamados de dentro da própria classe, e aí a anotação não passa pelo
 * proxy do Spring — a transação simplesmente não existiria.
 */
@Service
public class SincronizacaoCamaraService {

    private static final Logger log = LoggerFactory.getLogger(SincronizacaoCamaraService.class);

    private final CamaraApiClient api;
    private final ParlamentarRepository parlamentarRepository;
    private final PartidoRepository partidoRepository;
    private final ProposicaoRepository proposicaoRepository;
    private final ProposicaoAutorRepository proposicaoAutorRepository;
    private final VotacaoRepository votacaoRepository;
    private final VotoParlamentarRepository votoRepository;
    private final ParlamentarMapper parlamentarMapper;
    private final ProposicaoMapper proposicaoMapper;
    private final SincronizacaoProperties properties;
    private final TransactionTemplate transacao;

    public SincronizacaoCamaraService(CamaraApiClient api,
                                      ParlamentarRepository parlamentarRepository,
                                      PartidoRepository partidoRepository,
                                      ProposicaoRepository proposicaoRepository,
                                      ProposicaoAutorRepository proposicaoAutorRepository,
                                      VotacaoRepository votacaoRepository,
                                      VotoParlamentarRepository votoRepository,
                                      ParlamentarMapper parlamentarMapper,
                                      ProposicaoMapper proposicaoMapper,
                                      SincronizacaoProperties properties,
                                      PlatformTransactionManager transactionManager) {
        this.api = api;
        this.parlamentarRepository = parlamentarRepository;
        this.partidoRepository = partidoRepository;
        this.proposicaoRepository = proposicaoRepository;
        this.proposicaoAutorRepository = proposicaoAutorRepository;
        this.votacaoRepository = votacaoRepository;
        this.votoRepository = votoRepository;
        this.parlamentarMapper = parlamentarMapper;
        this.proposicaoMapper = proposicaoMapper;
        this.properties = properties;
        this.transacao = new TransactionTemplate(transactionManager);
        this.transacao.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public ResultadoSincronizacao sincronizar() {
        log.info("Sincronizando dados da Câmara dos Deputados");
        return sincronizarDeputados(true).mais(sincronizarVotacoes());
    }

    /**
     * Só quem são os deputados, sem o que assinaram. É o passo que precede a carga em massa: as
     * autorias e os votos dos arquivos só entram quando o parlamentar já existe no banco.
     */
    public ResultadoSincronizacao sincronizarParlamentares() {
        log.info("Sincronizando parlamentares da Câmara dos Deputados");
        return sincronizarDeputados(false);
    }

    private ResultadoSincronizacao sincronizarDeputados(boolean comProposicoes) {
        List<DeputadoApiResponse> deputados = api.listarDeputados();
        if (!properties.semLimiteDeParlamentares() && deputados.size() > properties.maxParlamentares()) {
            log.info("Limitando a {} de {} deputados nesta execução", properties.maxParlamentares(), deputados.size());
            deputados = deputados.subList(0, properties.maxParlamentares());
        }

        ResultadoSincronizacao total = ResultadoSincronizacao.vazio();
        for (DeputadoApiResponse deputado : deputados) {
            try {
                total = total.mais(transacao.execute(status -> gravarDeputado(deputado, comProposicoes)));
            } catch (RuntimeException e) {
                log.warn("Falha ao sincronizar o deputado {} ({}): {}", deputado.nome(), deputado.id(), e.toString());
                total = total.mais(new ResultadoSincronizacao(0, 0, 0, 0, 1));
            }
        }
        return total;
    }

    private ResultadoSincronizacao gravarDeputado(DeputadoApiResponse dto, boolean comProposicoes) {
        String idExterno = String.valueOf(dto.id());
        Parlamentar existente = parlamentarRepository.findByCasaAndIdExterno(Casa.CAMARA, idExterno).orElse(null);
        Parlamentar parlamentar = parlamentarMapper.aplicarDaCamara(existente, dto);
        parlamentar.setPartido(resolverPartido(dto.siglaPartido()));
        api.buscarDeputado(idExterno)
                .ifPresent(detalhe -> parlamentarMapper.aplicarDetalheDaCamara(parlamentar, detalhe));
        Parlamentar salvo = parlamentarRepository.save(parlamentar);

        int proposicoes = comProposicoes ? gravarProposicoesDe(salvo) : 0;
        return new ResultadoSincronizacao(1, proposicoes, 0, 0, 0);
    }

    private int gravarProposicoesDe(Parlamentar autor) {
        List<ProposicaoApiResponse> proposicoes = api.listarProposicoesPorAutor(autor.getIdExterno());
        if (proposicoes.size() > properties.maxProposicoesPorAutor()) {
            proposicoes = proposicoes.subList(0, properties.maxProposicoesPorAutor());
        }

        int gravadas = 0;
        for (ProposicaoApiResponse dto : proposicoes) {
            String idExterno = String.valueOf(dto.id());
            Proposicao existente = proposicaoRepository.findByCasaAndIdExterno(Casa.CAMARA, idExterno).orElse(null);
            Proposicao proposicao = proposicaoMapper.aplicarDaCamara(existente, dto);
            // O detalhe custa uma requisição por proposição, então só vale a pena quando a
            // situação ainda não é conhecida.
            if (properties.detalharProposicoes() && proposicao.getSituacao() == null) {
                api.buscarProposicao(idExterno)
                        .ifPresent(detalhe -> proposicaoMapper.aplicarDetalheDaCamara(proposicao, detalhe));
            }
            Proposicao salva = proposicaoRepository.save(proposicao);

            if (proposicaoAutorRepository.findByProposicaoAndParlamentar(salva, autor).isEmpty()) {
                proposicaoAutorRepository.save(new ProposicaoAutor(salva, autor, true));
            }
            gravadas++;
        }
        return gravadas;
    }

    private ResultadoSincronizacao sincronizarVotacoes() {
        LocalDate fim = LocalDate.now();
        LocalDate inicio = fim.minusDays(properties.diasDeVotacoes());
        List<VotacaoApiResponse> votacoes = api.listarVotacoes(inicio, fim);

        ResultadoSincronizacao total = ResultadoSincronizacao.vazio();
        for (VotacaoApiResponse dto : votacoes) {
            try {
                total = total.mais(transacao.execute(status -> gravarVotacao(dto)));
            } catch (RuntimeException e) {
                log.warn("Falha ao sincronizar a votação {}: {}", dto.id(), e.toString());
                total = total.mais(new ResultadoSincronizacao(0, 0, 0, 0, 1));
            }
        }
        return total;
    }

    private ResultadoSincronizacao gravarVotacao(VotacaoApiResponse dto) {
        Votacao existente = votacaoRepository.findByCasaAndIdExterno(Casa.CAMARA, dto.id()).orElse(null);
        Votacao votacao = proposicaoMapper.aplicarDaCamara(existente, dto);

        List<VotoApiResponse> votos = api.listarVotos(dto.id());
        // Lista vazia é votação simbólica: a casa deliberou sem apurar voto a voto. Guardar essa
        // distinção é o que permite a tela dizer "não houve voto nominal" em vez de deixar o
        // silêncio parecer omissão do parlamentar.
        votacao.setNominal(!votos.isEmpty());
        votacao.setSecreta(false);
        Votacao salva = votacaoRepository.save(votacao);

        int gravados = 0;
        for (VotoApiResponse voto : votos) {
            if (voto.deputado() == null || voto.deputado().id() == null) {
                continue;
            }
            String idDeputado = String.valueOf(voto.deputado().id());
            Parlamentar parlamentar = parlamentarRepository
                    .findByCasaAndIdExterno(Casa.CAMARA, idDeputado)
                    .orElse(null);
            if (parlamentar == null) {
                // Deputado fora da lista atual (suplente que assumiu, licenciado): entra quando a
                // sincronização de parlamentares alcançá-lo.
                continue;
            }
            if (votoRepository.findByVotacaoAndParlamentar(salva, parlamentar).isPresent()) {
                continue;
            }
            VotoParlamentar registro = new VotoParlamentar(salva, parlamentar,
                    proposicaoMapper.tipoVoto(voto.tipoVoto()));
            registro.setDescricaoOrigem(voto.tipoVoto());
            registro.setDataRegistro(Datas.paraInstante(voto.dataRegistroVoto()));
            votoRepository.save(registro);
            gravados++;
        }
        return new ResultadoSincronizacao(0, 0, 1, gravados, 0);
    }

    private Partido resolverPartido(String sigla) {
        if (sigla == null || sigla.isBlank()) {
            return null;
        }
        return partidoRepository.findBySigla(sigla)
                .orElseGet(() -> partidoRepository.save(new Partido(sigla)));
    }
}
