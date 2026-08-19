package com.deverdecasa.web;

import com.deverdecasa.dto.ParlamentarDtos.ParlamentarResumoDto;
import com.deverdecasa.service.DeputadoService;
import com.deverdecasa.service.FiltroBusca;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Telas de busca e de perfil.
 *
 * <p>Serve os dois caminhos com o mesmo handler: {@code /parlamentares}, que mistura as casas, e
 * {@code /deputados}, que continua valendo para não quebrar link já compartilhado e chega
 * restrito à Câmara quando ninguém pediu casa nenhuma. A distinção é só o valor padrão de um
 * filtro — a lógica de busca é uma só, no serviço.
 */
@Controller
public class ParlamentarController {

    private static final int TAMANHO_DA_PAGINA = 20;

    private final DeputadoService service;

    public ParlamentarController(DeputadoService service) {
        this.service = service;
    }

    @GetMapping("/")
    public String inicio() {
        return "redirect:/parlamentares";
    }

    /**
     * Busca com nome, partido e situação de aprovação combináveis entre si — todos opcionais, de
     * modo que a primeira visita já cai numa listagem cheia em vez de uma tela vazia esperando
     * alguém digitar.
     */
    @GetMapping({"/parlamentares", "/deputados"})
    public String buscar(@RequestParam(required = false) String nome,
                         @RequestParam(required = false) String partido,
                         @RequestParam(required = false) Boolean aprovadas,
                         @RequestParam(required = false) String casa,
                         @RequestParam(defaultValue = "0") int pagina,
                         HttpServletRequest request,
                         Model model) {
        FiltroBusca filtro = FiltroBusca.de(nome, partido, casaPadrao(casa, request), aprovadas);
        Page<ParlamentarResumoDto> resultado = service.buscar(filtro, pagina, TAMANHO_DA_PAGINA);

        model.addAttribute("resultado", resultado);
        model.addAttribute("filtro", filtro);
        model.addAttribute("nome", nome);
        model.addAttribute("partidos", service.partidosDisponiveis());
        return "busca";
    }

    /** Sem casa escolhida, /deputados fica na Câmara e /parlamentares abre para as duas. */
    private static String casaPadrao(String casaInformada, HttpServletRequest request) {
        if (casaInformada != null && !casaInformada.isBlank()) {
            return casaInformada;
        }
        return request.getRequestURI().startsWith("/deputados") ? "CAMARA" : null;
    }

    @GetMapping({"/parlamentares/{id}", "/deputados/{id}"})
    public String perfil(@PathVariable Long id, Model model) {
        model.addAttribute("perfil", service.perfil(id));
        return "perfil";
    }
}
