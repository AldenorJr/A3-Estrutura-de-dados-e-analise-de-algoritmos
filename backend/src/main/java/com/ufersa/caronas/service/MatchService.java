package com.ufersa.caronas.service;

import com.ufersa.caronas.model.MatchResult;
import com.ufersa.caronas.model.Rota;
import com.ufersa.caronas.model.Usuario;
import com.ufersa.caronas.structures.Grafo;
import com.ufersa.caronas.structures.QuickSort;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * O coracao do projeto: combina HASH + GRAFO + QUICKSORT para encontrar
 * caronas compativeis e ordena-las pelo melhor match.
 *
 * Pipeline (ver Apresentacao):
 *  1. Hash: localiza candidatos pelo bairro de origem em O(1).
 *  2. Hash: tambem traz candidatos dos BAIRROS VIZINHOS (expansao).
 *  3. Grafo: monta um grafo de compatibilidade e usa BFS para descobrir
 *     o "cluster" de pessoas conectadas ao solicitante.
 *  4. Filtros: universidade igual, horario compativel (janela configuravel),
 *     vagas > 0, motorista valido.
 *  5. QuickSort: ordena por score decrescente (melhor primeiro).
 */
@Service
public class MatchService {

    /** Janela aceitavel de diferenca de horario, em minutos. */
    private static final int JANELA_HORARIO_MIN = 30;

    private final UsuarioService usuarioService;
    private final RotaService rotaService;
    private final BairroService bairroService;

    public MatchService(UsuarioService usuarioService,
                        RotaService rotaService,
                        BairroService bairroService) {
        this.usuarioService = usuarioService;
        this.rotaService = rotaService;
        this.bairroService = bairroService;
    }

    /**
     * Busca caronas compativeis para um usuario, dado um horario desejado.
     *
     * @param solicitanteId  id do passageiro que quer carona
     * @param horarioDesejado horario alvo de saida
     * @param destinoFiltro   universidade alvo (opcional, default = a do usuario)
     */
    public List<MatchResult> buscarCaronas(Long solicitanteId,
                                           LocalTime horarioDesejado,
                                           String destinoFiltro) {
        Usuario solicitante = usuarioService.buscarPorId(solicitanteId);
        if (solicitante == null) return List.of();

        String universidadeAlvo = destinoFiltro != null && !destinoFiltro.isBlank()
                ? destinoFiltro : solicitante.getUniversidade();
        String bairroAlvo = solicitante.getBairro();

        // === ETAPA 1+2: HASH - busca candidatos por bairro e vizinhos ===
        Set<Rota> candidatosPorHash = new HashSet<>();
        candidatosPorHash.addAll(rotaService.buscarPorBairro(bairroAlvo));
        for (String vizinho : todosBairrosVizinhos(bairroAlvo)) {
            candidatosPorHash.addAll(rotaService.buscarPorBairro(vizinho));
        }

        // === ETAPA 3: GRAFO - monta grafo de compatibilidade e roda BFS ===
        Grafo<Usuario> grafo = construirGrafo(solicitante, candidatosPorHash, horarioDesejado, universidadeAlvo);
        List<Usuario> alcancaveis = grafo.buscaEmLargura(solicitante);
        Set<Long> idsAlcancaveis = new HashSet<>();
        for (Usuario u : alcancaveis) idsAlcancaveis.add(u.getId());

        // === ETAPA 4: filtros adicionais ===
        List<MatchResult> matches = new ArrayList<>();
        for (Rota r : candidatosPorHash) {
            if (r.getUsuarioId().equals(solicitanteId)) continue;            // nao auto-match
            if (r.getVagasDisponiveis() <= 0) continue;
            if (!idsAlcancaveis.contains(r.getUsuarioId())) continue;        // grafo nao alcanca

            Usuario motorista = usuarioService.buscarPorId(r.getUsuarioId());
            if (motorista == null || !motorista.isMotorista()) continue;
            if (!sameUni(motorista.getUniversidade(), universidadeAlvo)) continue;

            int diffMin = diferencaMinutos(r.getHorarioSaida(), horarioDesejado);
            if (diffMin > JANELA_HORARIO_MIN) continue;

            int distBairro = bairroService.distancia(bairroAlvo, r.getBairroOrigem());
            double score = calcularScore(motorista, diffMin, distBairro, r.getVagasDisponiveis());
            String compat = explicarCompatibilidade(distBairro, diffMin, motorista);

            matches.add(new MatchResult(motorista, r, score, diffMin, compat));
        }

        // === ETAPA 5: QUICKSORT - ordena por score decrescente ===
        QuickSort.ordenar(matches, Comparator.comparingDouble(MatchResult::getScore).reversed());

        return matches;
    }

    private Grafo<Usuario> construirGrafo(Usuario solicitante,
                                          Set<Rota> candidatos,
                                          LocalTime horarioDesejado,
                                          String universidadeAlvo) {
        Grafo<Usuario> g = new Grafo<>();
        g.addNo(solicitante);

        Set<Long> idsCandidatos = new HashSet<>();
        for (Rota r : candidatos) idsCandidatos.add(r.getUsuarioId());

        // Pega objetos Usuario unicos dos candidatos
        List<Usuario> usuariosCandidatos = new ArrayList<>();
        for (Long id : idsCandidatos) {
            Usuario u = usuarioService.buscarPorId(id);
            if (u != null) usuariosCandidatos.add(u);
        }

        // Cria aresta solicitante <-> candidato se compatibilidade basica passa
        for (Usuario c : usuariosCandidatos) {
            if (c.getId().equals(solicitante.getId())) continue;
            if (!sameUni(c.getUniversidade(), universidadeAlvo)) continue;
            if (!bairroService.saoProximos(solicitante.getBairro(), c.getBairro())) continue;

            // Pelo menos uma rota do candidato deve estar dentro da janela
            boolean temHorarioOk = false;
            for (Rota r : rotaService.buscarPorUsuario(c.getId())) {
                if (diferencaMinutos(r.getHorarioSaida(), horarioDesejado) <= JANELA_HORARIO_MIN) {
                    temHorarioOk = true;
                    break;
                }
            }
            if (!temHorarioOk) continue;

            g.addAresta(solicitante, c);
        }

        // Conecta candidatos entre si (transitividade - ajuda a formar clusters)
        for (int i = 0; i < usuariosCandidatos.size(); i++) {
            for (int j = i + 1; j < usuariosCandidatos.size(); j++) {
                Usuario a = usuariosCandidatos.get(i);
                Usuario b = usuariosCandidatos.get(j);
                if (sameUni(a.getUniversidade(), b.getUniversidade())
                        && bairroService.saoProximos(a.getBairro(), b.getBairro())) {
                    g.addAresta(a, b);
                }
            }
        }
        return g;
    }

    /**
     * Score de 0 a 100:
     *   - Avaliacao do motorista (peso 40)
     *   - Proximidade de bairro    (peso 30)
     *   - Compatibilidade horaria  (peso 25)
     *   - Disponibilidade de vagas (peso 5)
     */
    private double calcularScore(Usuario motorista, int diffMin, int distBairro, int vagas) {
        double avaliacaoNorm = motorista.getAvaliacao() / 5.0; // 0..1
        double bairroNorm = distBairro == 0 ? 1.0 : (distBairro == 1 ? 0.7 : 0.3);
        double horarioNorm = 1.0 - (diffMin / (double) JANELA_HORARIO_MIN);
        double vagasNorm = Math.min(vagas / 4.0, 1.0);
        return (avaliacaoNorm * 40) + (bairroNorm * 30) + (horarioNorm * 25) + (vagasNorm * 5);
    }

    private String explicarCompatibilidade(int distBairro, int diffMin, Usuario motorista) {
        StringBuilder sb = new StringBuilder();
        sb.append(distBairro == 0 ? "Mesmo bairro" : (distBairro == 1 ? "Bairro vizinho" : "Bairro proximo"));
        sb.append(" - diferenca de ").append(diffMin).append(" min");
        if (motorista.getTotalAvaliacoes() > 0) {
            sb.append(" - ").append(String.format("%.1f", motorista.getAvaliacao())).append("★");
        }
        return sb.toString();
    }

    private int diferencaMinutos(LocalTime a, LocalTime b) {
        if (a == null || b == null) return Integer.MAX_VALUE;
        int min = Math.abs(a.toSecondOfDay() - b.toSecondOfDay()) / 60;
        return min;
    }

    private boolean sameUni(String a, String b) {
        return a != null && b != null && a.trim().equalsIgnoreCase(b.trim());
    }

    private List<String> todosBairrosVizinhos(String bairro) {
        List<String> r = new ArrayList<>();
        for (String b : bairroService.todosBairros()) {
            if (!b.equalsIgnoreCase(bairro) && bairroService.saoProximos(bairro, b)) {
                r.add(b);
            }
        }
        return r;
    }
}
