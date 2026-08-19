package com.deverdecasa.web;

import com.deverdecasa.dto.ParlamentarDtos.ParlamentarResumoDto;
import com.deverdecasa.service.DeputadoService;
import com.deverdecasa.service.FiltroBusca;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Telas de busca e de perfil. */
@Controller
public class DeputadoController {

    private static final int TAMANHO_DA_PAGINA = 20;

    private final DeputadoService service;

    public DeputadoController(DeputadoService service) {
        this.service = service;
    }

    @GetMapping("/")
    public String inicio() {
        return "redirect:/deputados";
    }

    /**
     * Busca com nome, partido e situação de aprovação combináveis entre si — todos opcionais, de
     * modo que a primeira visita já cai numa listagem cheia em vez de uma tela vazia esperando
     * alguém digitar.
     */
    @GetMapping("/deputados")
    public String buscar(@RequestParam(required = false) String nome,
                         @RequestParam(required = false) String partido,
                         @RequestParam(required = false) Boolean aprovadas,
                         @RequestParam(required = false) String casa,
                         @RequestParam(defaultValue = "0") int pagina,
                         Model model) {
        FiltroBusca filtro = FiltroBusca.de(nome, partido, casa, aprovadas);
        Page<ParlamentarResumoDto> resultado = service.buscar(filtro, pagina, TAMANHO_DA_PAGINA);

        model.addAttribute("resultado", resultado);
        model.addAttribute("filtro", filtro);
        model.addAttribute("nome", nome);
        model.addAttribute("partidos", service.partidosDisponiveis());
        return "busca";
    }

    @GetMapping("/deputados/{id}")
    public String perfil(@PathVariable Long id, Model model) {
        model.addAttribute("perfil", service.perfil(id));
        return "perfil";
    }
}
