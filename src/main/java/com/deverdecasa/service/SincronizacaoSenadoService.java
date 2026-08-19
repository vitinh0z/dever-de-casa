package com.deverdecasa.service;

import com.deverdecasa.domain.Casa;
import com.deverdecasa.domain.Parlamentar;
import com.deverdecasa.domain.Partido;
import com.deverdecasa.domain.Proposicao;
import com.deverdecasa.domain.ProposicaoAutor;
import com.deverdecasa.domain.Votacao;
import com.deverdecasa.domain.VotoParlamentar;
import com.deverdecasa.integracao.senado.SenadoApiClient;
import com.deverdecasa.integracao.senado.SenadoDtos.IdentificacaoParlamentar;
import com.deverdecasa.integracao.senado.SenadoDtos.ProcessoApiResponse;
import com.deverdecasa.integracao.senado.SenadoDtos.VotacaoApiResponse;
import com.deverdecasa.integracao.senado.SenadoDtos.VotoApiResponse;
import com.deverdecasa.mapper.ProposicaoMapper;
import com.deverdecasa.mapper.SenadoMapper;
import com.deverdecasa.repository.ParlamentarRepository;
import com.deverdecasa.repository.PartidoRepository;
import com.deverdecasa.repository.ProposicaoAutorRepository;
import com.deverdecasa.repository.ProposicaoRepository;
import com.deverdecasa.repository.VotacaoRepository;
import com.deverdecasa.repository.VotoParlamentarRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Traz para o banco local o que o Senado publica, gravando nas mesmas tabelas que a Câmara
 * alimenta.
 *
 * <p>Roda independente da Câmara: uma casa fora do ar não impede a outra de atualizar. A diferença
 * de formato entre as duas fontes já foi resolvida no {@link SenadoMapper}, então o que sobra aqui
 * é o mesmo roteiro — resolver partido, gravar parlamentar, matérias e votos.
 */
@Service
public class SincronizacaoSenadoService {

    private static final Logger log = LoggerFactory.getLogger(SincronizacaoSenadoService.class);

    private final SenadoApiClient api;
    private final ParlamentarRepository parlamentarRepository;
    private final PartidoRepository partidoRepository;
    private final ProposicaoRepository proposicaoRepository;
    private final ProposicaoAutorRepository proposicaoAutorRepository;
    private final VotacaoRepository votacaoRepository;
    private final VotoParlamentarRepository votoRepository;
    private final SenadoMapper senadoMapper;
    private final ProposicaoMapper proposicaoMapper;
    private final SincronizacaoProperties properties;
    private final TransactionTemplate transacao;

    public SincronizacaoSenadoService(SenadoApiClient api,
                                      ParlamentarRepository parlamentarRepository,
                                      PartidoRepository partidoRepository,
                                      ProposicaoRepository proposicaoRepository,
                                      ProposicaoAutorRepository proposicaoAutorRepository,
                                      VotacaoRepository votacaoRepository,
                                      VotoParlamentarRepository votoRepository,
                                      SenadoMapper senadoMapper,
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
        this.senadoMapper = senadoMapper;
        this.proposicaoMapper = proposicaoMapper;
        this.properties = properties;
        this.transacao = new TransactionTemplate(transactionManager);
        this.transacao.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public ResultadoSincronizacao sincronizar() {
        log.info("Sincronizando dados do Senado Federal");
        return sincronizarSenadores(true).mais(sincronizarVotacoes());
    }

    /** Só quem são os senadores, sem as matérias que assinaram. */
    public ResultadoSincronizacao sincronizarParlamentares() {
        log.info("Sincronizando parlamentares do Senado Federal");
        return sincronizarSenadores(false);
    }

    private ResultadoSincronizacao sincronizarSenadores(boolean comMaterias) {
        List<IdentificacaoParlamentar> senadores = api.listarSenadores();
        if (!properties.semLimiteDeParlamentares() && senadores.size() > properties.maxParlamentares()) {
            log.info("Limitando a {} de {} senadores nesta execução", properties.maxParlamentares(), senadores.size());
            senadores = senadores.subList(0, properties.maxParlamentares());
        }

        ResultadoSincronizacao total = ResultadoSincronizacao.vazio();
        for (IdentificacaoParlamentar senador : senadores) {
            try {
                total = total.mais(transacao.execute(status -> gravarSenador(senador, comMaterias)));
            } catch (RuntimeException e) {
                log.warn("Falha ao sincronizar o senador {} ({}): {}",
                        senador.nomeParlamentar(), senador.codigoParlamentar(), e.toString());
                total = total.mais(new ResultadoSincronizacao(0, 0, 0, 0, 1));
            }
        }
        return total;
    }

    private ResultadoSincronizacao gravarSenador(IdentificacaoParlamentar dto, boolean comMaterias) {
        Parlamentar existente = parlamentarRepository
                .findByCasaAndIdExterno(Casa.SENADO, dto.codigoParlamentar())
                .orElse(null);
        Parlamentar parlamentar = senadoMapper.aplicar(existente, dto);
        parlamentar.setPartido(resolverPartido(dto.siglaPartido()));
        Parlamentar salvo = parlamentarRepository.save(parlamentar);

        int materias = comMaterias ? gravarMateriasDe(salvo) : 0;
        return new ResultadoSincronizacao(1, materias, 0, 0, 0);
    }

    private int gravarMateriasDe(Parlamentar autor) {
        List<ProcessoApiResponse> materias = api.listarMateriasPorAutor(autor.getIdExterno());
        if (materias.size() > properties.maxProposicoesPorAutor()) {
            materias = materias.subList(0, properties.maxProposicoesPorAutor());
        }

        int gravadas = 0;
        for (ProcessoApiResponse dto : materias) {
            if (dto.id() == null) {
                continue;
            }
            String idExterno = String.valueOf(dto.id());
            Proposicao existente = proposicaoRepository.findByCasaAndIdExterno(Casa.SENADO, idExterno).orElse(null);
            Proposicao salva = proposicaoRepository.save(senadoMapper.aplicar(existente, dto));

            if (proposicaoAutorRepository.findByProposicaoAndParlamentar(salva, autor).isEmpty()) {
                proposicaoAutorRepository.save(new ProposicaoAutor(salva, autor, true));
            }
            gravadas++;
        }
        return gravadas;
    }

    private ResultadoSincronizacao sincronizarVotacoes() {
        List<VotacaoApiResponse> votacoes = api.listarVotacoes();

        ResultadoSincronizacao total = ResultadoSincronizacao.vazio();
        for (VotacaoApiResponse dto : votacoes) {
            if (dto.codigoSessaoVotacao() == null) {
                continue;
            }
            try {
                total = total.mais(transacao.execute(status -> gravarVotacao(dto)));
            } catch (RuntimeException e) {
                log.warn("Falha ao sincronizar a votação {} do Senado: {}", dto.codigoSessaoVotacao(), e.toString());
                total = total.mais(new ResultadoSincronizacao(0, 0, 0, 0, 1));
            }
        }
        return total;
    }

    private ResultadoSincronizacao gravarVotacao(VotacaoApiResponse dto) {
        String idExterno = String.valueOf(dto.codigoSessaoVotacao());
        Votacao existente = votacaoRepository.findByCasaAndIdExterno(Casa.SENADO, idExterno).orElse(null);
        Votacao salva = votacaoRepository.save(senadoMapper.aplicar(existente, dto));

        int gravados = 0;
        for (VotoApiResponse voto : dto.votosOuVazio()) {
            if (voto.codigoParlamentar() == null) {
                continue;
            }
            Parlamentar parlamentar = parlamentarRepository
                    .findByCasaAndIdExterno(Casa.SENADO, String.valueOf(voto.codigoParlamentar()))
                    .orElse(null);
            if (parlamentar == null) {
                // Senador de mandato anterior ou suplente ainda não sincronizado.
                continue;
            }
            if (votoRepository.findByVotacaoAndParlamentar(salva, parlamentar).isPresent()) {
                continue;
            }
            VotoParlamentar registro = new VotoParlamentar(salva, parlamentar,
                    proposicaoMapper.tipoVoto(voto.siglaVotoParlamentar()));
            registro.setDescricaoOrigem(voto.siglaVotoParlamentar());
            votoRepository.save(registro);
            gravados++;
        }
        return new ResultadoSincronizacao(0, 0, 1, gravados, 0);
    }

    private Partido resolverPartido(String sigla) {
        if (sigla == null || sigla.isBlank()) {
            return null;
        }
        String limpa = sigla.trim();
        return partidoRepository.findBySigla(limpa)
                .orElseGet(() -> partidoRepository.save(new Partido(limpa)));
    }
}
