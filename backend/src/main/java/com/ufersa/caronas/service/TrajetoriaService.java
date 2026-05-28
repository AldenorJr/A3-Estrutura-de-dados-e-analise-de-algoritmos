package com.ufersa.caronas.service;

import com.ufersa.caronas.model.Coordenada;
import com.ufersa.caronas.model.Rota;
import com.ufersa.caronas.model.TrajetoriaResult;
import com.ufersa.caronas.model.Usuario;
import com.ufersa.caronas.structures.Dijkstra;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Calcula a "melhor trajetoria" da corrida combinando:
 *
 *   PARTIDA (motorista)  ──Dijkstra──▶  PICKUP (passageiro)  ──Dijkstra──▶  UFERSA
 *
 * Cada perna do trajeto e o caminho minimo no grafo ponderado de bairros.
 * O resultado inclui sequencia de paradas, lista de coordenadas para
 * desenhar a polyline no mapa, distancia total em km, tempo estimado e
 * uma estimativa de economia (vs. UBER).
 */
@Service
public class TrajetoriaService {

    /** Velocidade media estimada em area urbana de Mossoro (km/h). */
    private static final double VELOCIDADE_MEDIA_KMH = 30.0;
    /** Preco medio do km via app (R$/km). Base para a economia estimada. */
    private static final double PRECO_KM_UBER = 2.50;

    private final UsuarioService usuarioService;
    private final RotaService rotaService;
    private final BairroService bairroService;

    public TrajetoriaService(UsuarioService usuarioService,
                             RotaService rotaService,
                             BairroService bairroService) {
        this.usuarioService = usuarioService;
        this.rotaService = rotaService;
        this.bairroService = bairroService;
    }

    public TrajetoriaResult calcular(Long passageiroId, Long rotaId) {
        Usuario passageiro = usuarioService.buscarPorId(passageiroId);
        Rota rota = rotaService.buscarPorId(rotaId);
        if (passageiro == null || rota == null) return null;
        Usuario motorista = usuarioService.buscarPorId(rota.getUsuarioId());
        if (motorista == null) return null;

        String origemMotorista = rota.getBairroOrigem();
        String origemPassageiro = passageiro.getBairro();
        String destino = BairroService.DESTINO_UFERSA;

        // === Dijkstra perna 1: motorista -> passageiro ===
        Dijkstra.Resultado<String> perna1 = bairroService.melhorTrajeto(origemMotorista, origemPassageiro);
        // === Dijkstra perna 2: passageiro -> UFERSA ===
        Dijkstra.Resultado<String> perna2 = bairroService.melhorTrajeto(origemPassageiro, destino);

        TrajetoriaResult result = new TrajetoriaResult();
        result.algoritmoUsado = "Dijkstra (caminho minimo em grafo ponderado)";

        // junta os bairros sem duplicar o ponto de pickup
        List<String> bairros = new ArrayList<>(perna1.caminho);
        if (!perna2.caminho.isEmpty()) {
            // primeiro elemento de perna2 == ultimo de perna1 (pickup)
            for (int i = 1; i < perna2.caminho.size(); i++) {
                bairros.add(perna2.caminho.get(i));
            }
        }
        result.bairrosCaminho = bairros;

        // Constroi a polyline (coordenadas de cada bairro do caminho)
        List<Coordenada> poligono = new ArrayList<>();
        for (String b : bairros) {
            Coordenada c = bairroService.coordenadaDe(b);
            if (c != null) poligono.add(c);
        }
        result.caminhoPoligono = poligono;

        // Paradas principais (PARTIDA, PICKUP, DESTINO)
        List<TrajetoriaResult.Parada> paradas = new ArrayList<>();
        paradas.add(new TrajetoriaResult.Parada(
                origemMotorista, "PARTIDA",
                motorista.getNome() + " sai de " + origemMotorista,
                bairroService.coordenadaDe(origemMotorista)));
        if (!origemMotorista.equalsIgnoreCase(origemPassageiro)) {
            paradas.add(new TrajetoriaResult.Parada(
                    origemPassageiro, "PICKUP",
                    "Pega " + passageiro.getNome() + " em " + origemPassageiro,
                    bairroService.coordenadaDe(origemPassageiro)));
        }
        paradas.add(new TrajetoriaResult.Parada(
                destino, "DESTINO",
                "Chega na " + destino,
                bairroService.coordenadaDe(destino)));
        result.paradas = paradas;

        // Distancia total = soma de cada segmento da polyline (Haversine)
        double km = 0.0;
        for (int i = 1; i < poligono.size(); i++) {
            km += poligono.get(i - 1).distanciaKm(poligono.get(i));
        }
        result.distanciaTotalKm = round1(km);
        result.tempoEstimadoMin = (int) Math.round((km / VELOCIDADE_MEDIA_KMH) * 60.0);

        // Economia: o que custaria pegar Uber/99 sozinho do bairro do passageiro
        // ate UFERSA, vs vir de carona gratis (rateio de combustivel desconsiderado).
        double kmPassageiroSozinho = perna2.alcancou()
                ? somaSegmentos(perna2.caminho)
                : km;
        result.economiaEstimadaReais = round2(kmPassageiroSozinho * PRECO_KM_UBER);

        return result;
    }

    private double somaSegmentos(List<String> caminho) {
        double s = 0;
        for (int i = 1; i < caminho.size(); i++) {
            Coordenada a = bairroService.coordenadaDe(caminho.get(i - 1));
            Coordenada b = bairroService.coordenadaDe(caminho.get(i));
            if (a != null && b != null) s += a.distanciaKm(b);
        }
        return s;
    }

    private static double round1(double v) { return Math.round(v * 10.0) / 10.0; }
    private static double round2(double v) { return Math.round(v * 100.0) / 100.0; }
}
