package com.deverdecasa.service;

import com.deverdecasa.domain.Parlamentar;
import com.deverdecasa.dto.ParlamentarDtos.ParlamentarResumoDto;
import com.deverdecasa.dto.ParlamentarDtos.PerfilDto;
import com.deverdecasa.dto.ParlamentarDtos.ProposicaoDto;
import com.deverdecasa.dto.ParlamentarDtos.VotacaoSimbolicaDto;
import com.deverdecasa.dto.ParlamentarDtos.VotoDto;
import com.deverdecasa.mapper.ParlamentarMapper;
import com.deverdecasa.mapper.ProposicaoMapper;
import com.deverdecasa.repository.ParlamentarRepository;
import com.deverdecasa.repository.PartidoRepository;
import com.deverdecasa.repository.ProposicaoAutorRepository;
import com.deverdecasa.repository.VotacaoRepository;
import com.deverdecasa.repository.VotoParlamentarRepository;
import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consultas que as telas fazem: buscar parlamentares e montar o perfil de um deles.
 *
 * <p>O que entra em cache é a busca, que é a tela mais visitada e cujo resultado só muda quando a
 * sincronização roda. O perfil monta listas grandes e varia por pessoa, então fica de fora para
 * não encher a memória com pouco reaproveitamento.
 */
@Service
@Transactional(readOnly = true)
public class DeputadoService {

    /** Teto do histórico exibido: o perfil precisa abrir rápido, não listar um mandato inteiro. */
    private static final int MAX_VOTOS_NO_PERFIL = 100;
    private static final int MAX_SIMBOLICAS_NO_PERFIL = 20;

    private final ParlamentarRepository parlamentarRepository;
    private final PartidoRepository partidoRepository;
    private final ProposicaoAutorRepository proposicaoAutorRepository;
    private final VotoParlamentarRepository votoRepository;
    private final VotacaoRepository votacaoRepository;
    private final ParlamentarMapper parlamentarMapper;
    private final ProposicaoMapper proposicaoMapper;

    public DeputadoService(ParlamentarRepository parlamentarRepository,
                           PartidoRepository partidoRepository,
                           ProposicaoAutorRepository proposicaoAutorRepository,
                           VotoParlamentarRepository votoRepository,
                           VotacaoRepository votacaoRepository,
                           ParlamentarMapper parlamentarMapper,
                           ProposicaoMapper proposicaoMapper) {
        this.parlamentarRepository = parlamentarRepository;
        this.partidoRepository = partidoRepository;
        this.proposicaoAutorRepository = proposicaoAutorRepository;
        this.votoRepository = votoRepository;
        this.votacaoRepository = votacaoRepository;
        this.parlamentarMapper = parlamentarMapper;
        this.proposicaoMapper = proposicaoMapper;
    }

    /** Busca por nome com os filtros combinados entre si. */
    @Cacheable(value = "buscaParlamentares",
            key = "#filtro.toString() + '|' + #pagina + '|' + #tamanho")
    public Page<ParlamentarResumoDto> buscar(FiltroBusca filtro, int pagina, int tamanho) {
        Pageable pageable = PageRequest.of(Math.max(pagina, 0), tamanho);
        return parlamentarRepository
                .buscar(filtro.termo(), filtro.partido(), filtro.casaComoTexto(), filtro.aprovadas(), pageable)
                .map(parlamentarMapper::paraResumo);
    }

    /** Siglas oferecidas no filtro de partido. */
    @Cacheable("partidos")
    public List<String> partidosDisponiveis() {
        return partidoRepository.siglasComParlamentar();
    }

    /**
     * Perfil completo: quem é, o que assinou e como votou.
     *
     * <p>Junto com os votos vêm as votações simbólicas que decidiram propostas dele. Sem essa
     * lista, quem abrisse o perfil de alguém cujas propostas só passaram por acordo veria um
     * histórico vazio e entenderia ausência onde houve deliberação sem voto nominal.
     */
    public PerfilDto perfil(Long id) {
        Parlamentar parlamentar = parlamentarRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.parlamentar(id));

        List<ProposicaoDto> proposicoes = proposicaoAutorRepository.doParlamentar(id).stream()
                .map(proposicaoMapper::paraDto)
                .toList();
        List<VotoDto> votos = votoRepository
                .historicoDoParlamentar(id, PageRequest.of(0, MAX_VOTOS_NO_PERFIL)).stream()
                .map(proposicaoMapper::paraDto)
                .toList();
        List<VotacaoSimbolicaDto> simbolicas = votacaoRepository
                .simbolicasDasProposicoesDoParlamentar(id, PageRequest.of(0, MAX_SIMBOLICAS_NO_PERFIL)).stream()
                .map(proposicaoMapper::paraSimbolicaDto)
                .toList();

        return new PerfilDto(
                parlamentar.getId(),
                parlamentar.getNome(),
                parlamentar.getNomeCivil(),
                parlamentar.getCasa(),
                parlamentar.getSiglaUf(),
                parlamentar.getPartido() == null ? null : parlamentar.getPartido().getSigla(),
                parlamentar.getUrlFoto(),
                parlamentar.getEmail(),
                parlamentar.getSituacao(),
                proposicoes,
                votos,
                simbolicas);
    }
}
