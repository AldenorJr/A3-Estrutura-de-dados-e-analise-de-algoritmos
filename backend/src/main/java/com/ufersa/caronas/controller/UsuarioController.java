package com.ufersa.caronas.controller;

import com.ufersa.caronas.dto.AvaliacaoDTO;
import com.ufersa.caronas.dto.UsuarioDTO;
import com.ufersa.caronas.model.Usuario;
import com.ufersa.caronas.model.Veiculo;
import com.ufersa.caronas.service.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping
    public ResponseEntity<Usuario> criar(@RequestBody UsuarioDTO dto) {
        Veiculo v = null;
        if (dto.motorista && dto.veiculoModelo != null) {
            v = new Veiculo(dto.veiculoModelo, dto.veiculoPlaca, dto.veiculoCor,
                    dto.veiculoVagas == null ? 1 : dto.veiculoVagas);
        }
        Usuario u = new Usuario(dto.nome, dto.email, dto.curso, dto.bairro,
                dto.universidade, dto.motorista, v);
        return ResponseEntity.ok(usuarioService.salvar(u));
    }

    @GetMapping
    public List<Usuario> listar() {
        return usuarioService.listarTodos();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Usuario> buscar(@PathVariable Long id) {
        Usuario u = usuarioService.buscarPorId(id);
        return u == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(u);
    }

    @GetMapping("/bairro/{bairro}")
    public List<Usuario> porBairro(@PathVariable String bairro) {
        return usuarioService.buscarPorBairro(bairro);
    }

    @PostMapping("/avaliar")
    public ResponseEntity<Usuario> avaliar(@RequestBody AvaliacaoDTO dto) {
        Usuario u = usuarioService.buscarPorId(dto.motoristaId);
        if (u == null) return ResponseEntity.notFound().build();
        if (dto.nota == null || dto.nota < 0 || dto.nota > 5) {
            return ResponseEntity.badRequest().build();
        }
        u.registrarAvaliacao(dto.nota);
        return ResponseEntity.ok(u);
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        return Map.of(
                "totalUsuarios", usuarioService.total(),
                "totalBairros", usuarioService.totalBairros(),
                "baldesOcupadosNoIndiceBairro", usuarioService.baldesUsadosBairro()
        );
    }
}
