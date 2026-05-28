package com.ufersa.caronas.controller;

import com.ufersa.caronas.model.Coordenada;
import com.ufersa.caronas.model.TrajetoriaResult;
import com.ufersa.caronas.service.BairroService;
import com.ufersa.caronas.service.TrajetoriaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/trajetoria")
public class TrajetoriaController {

    private final TrajetoriaService trajetoriaService;
    private final BairroService bairroService;

    public TrajetoriaController(TrajetoriaService trajetoriaService,
                                BairroService bairroService) {
        this.trajetoriaService = trajetoriaService;
        this.bairroService = bairroService;
    }

    /**
     * GET /api/trajetoria?passageiroId=X&rotaId=Y
     * Calcula a melhor trajetoria (Dijkstra) entre motorista da rota,
     * o passageiro solicitante e a UFERSA.
     */
    @GetMapping
    public ResponseEntity<TrajetoriaResult> calcular(@RequestParam Long passageiroId,
                                                     @RequestParam Long rotaId) {
        TrajetoriaResult r = trajetoriaService.calcular(passageiroId, rotaId);
        return r == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(r);
    }

    /** GET /api/trajetoria/coordenadas - util pro frontend desenhar o mapa de bairros. */
    @GetMapping("/coordenadas")
    public Map<String, Coordenada> coordenadas() {
        return bairroService.todasCoordenadas();
    }
}
