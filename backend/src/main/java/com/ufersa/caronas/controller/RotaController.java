package com.ufersa.caronas.controller;

import com.ufersa.caronas.dto.RotaDTO;
import com.ufersa.caronas.model.Rota;
import com.ufersa.caronas.model.TipoRota;
import com.ufersa.caronas.service.RotaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;

@RestController
@RequestMapping("/api/rotas")
public class RotaController {

    private final RotaService rotaService;

    public RotaController(RotaService rotaService) {
        this.rotaService = rotaService;
    }

    @PostMapping
    public ResponseEntity<?> criar(@RequestBody RotaDTO dto) {
        LocalTime horario;
        try {
            horario = LocalTime.parse(dto.horarioSaida);
        } catch (DateTimeParseException ex) {
            return ResponseEntity.badRequest().body("Horario invalido (use HH:mm)");
        }
        TipoRota tipo;
        try {
            tipo = TipoRota.valueOf(dto.tipo == null ? "IDA" : dto.tipo.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body("Tipo invalido (IDA ou VOLTA)");
        }

        Rota r = new Rota(dto.usuarioId, dto.bairroOrigem, dto.destino,
                horario, dto.vagasDisponiveis == null ? 1 : dto.vagasDisponiveis, tipo);
        return ResponseEntity.ok(rotaService.salvar(r));
    }

    @GetMapping
    public List<Rota> listar() { return rotaService.listarTodas(); }

    @GetMapping("/usuario/{usuarioId}")
    public List<Rota> porUsuario(@PathVariable Long usuarioId) {
        return rotaService.buscarPorUsuario(usuarioId);
    }

    @GetMapping("/bairro/{bairro}")
    public List<Rota> porBairro(@PathVariable String bairro) {
        return rotaService.buscarPorBairro(bairro);
    }
}
