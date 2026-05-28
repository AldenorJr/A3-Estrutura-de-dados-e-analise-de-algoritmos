package com.ufersa.caronas.controller;

import com.ufersa.caronas.service.BairroService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RestController
public class BairroController {

    private final BairroService bairroService;

    public BairroController(BairroService bairroService) {
        this.bairroService = bairroService;
    }

    @GetMapping("/api/bairros")
    public List<String> listarBairros() {
        List<String> lista = new ArrayList<>(bairroService.todosBairros());
        Collections.sort(lista);
        return lista;
    }

    @GetMapping("/api/universidades")
    public List<String> listarUniversidades() {
        return new ArrayList<>(bairroService.universidades());
    }
}
