package com.ufersa.caronas.controller;

import com.ufersa.caronas.service.BairroService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/bairros")
public class BairroController {

    private final BairroService bairroService;

    public BairroController(BairroService bairroService) {
        this.bairroService = bairroService;
    }

    @GetMapping
    public List<String> listar() {
        List<String> lista = new ArrayList<>(bairroService.todosBairros());
        Collections.sort(lista);
        return lista;
    }
}
