package com.ufersa.caronas.controller;

import com.ufersa.caronas.model.MatchResult;
import com.ufersa.caronas.service.MatchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/match")
public class MatchController {

    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    /**
     * Busca caronas compativeis.
     * Exemplo: GET /api/match?usuarioId=1&horario=07:30
     */
    @GetMapping
    public List<MatchResult> buscar(@RequestParam Long usuarioId,
                                    @RequestParam String horario,
                                    @RequestParam(required = false) String destino) {
        LocalTime h = LocalTime.parse(horario);
        return matchService.buscarCaronas(usuarioId, h, destino);
    }
}
