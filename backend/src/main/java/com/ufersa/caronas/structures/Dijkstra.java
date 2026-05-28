package com.ufersa.caronas.structures;

import java.util.*;

/**
 * Algoritmo de Dijkstra para caminho minimo em grafo ponderado
 * com pesos NAO negativos.
 *
 * Decisao academica: NAO usamos uma biblioteca de grafo pronta.
 * A nossa implementacao opera sobre {@link GrafoPonderado}.
 *
 * Como funciona, em etapas:
 *  1. Inicializa todas as distancias como infinito, exceto a origem (0).
 *  2. Coloca a origem em uma fila de prioridade (min-heap) por distancia.
 *  3. Enquanto a fila nao estiver vazia:
 *       a) Pega o no com menor distancia atual.
 *       b) Para cada vizinho, calcula a nova distancia (atual + peso).
 *          Se for melhor que a conhecida, atualiza e enfileira.
 *  4. Quando o destino e desenfileirado, sabemos a distancia minima
 *     e podemos reconstruir o caminho.
 *
 * Complexidade:
 *   - com PriorityQueue (heap binario): O((V + E) log V)
 *   - sem heap (busca linear pelo proximo):    O(V^2)
 *
 * Para a nossa malha de bairros (poucas dezenas de nos), e instantaneo.
 *
 * @param <T> tipo dos nos
 */
public final class Dijkstra {

    private Dijkstra() {}

    public static class Resultado<T> {
        public final List<T> caminho;
        public final double distanciaTotal;

        public Resultado(List<T> caminho, double distanciaTotal) {
            this.caminho = caminho;
            this.distanciaTotal = distanciaTotal;
        }

        public boolean alcancou() {
            return !caminho.isEmpty() && distanciaTotal < Double.POSITIVE_INFINITY;
        }
    }

    /**
     * Calcula o caminho minimo entre origem e destino no grafo.
     * Retorna a lista ordenada de nos do caminho e a distancia total.
     */
    public static <T> Resultado<T> caminhoMinimo(GrafoPonderado<T> grafo, T origem, T destino) {
        if (!grafo.contem(origem) || !grafo.contem(destino)) {
            return new Resultado<>(List.of(), Double.POSITIVE_INFINITY);
        }
        if (origem.equals(destino)) {
            return new Resultado<>(List.of(origem), 0.0);
        }

        Map<T, Double> dist = new HashMap<>();
        Map<T, T> anterior = new HashMap<>();
        Set<T> finalizados = new HashSet<>();

        for (T no : grafo.nos()) {
            dist.put(no, Double.POSITIVE_INFINITY);
        }
        dist.put(origem, 0.0);

        // min-heap por distancia
        PriorityQueue<T> fila = new PriorityQueue<>(Comparator.comparingDouble(a -> dist.getOrDefault(a, Double.POSITIVE_INFINITY)));
        fila.add(origem);

        while (!fila.isEmpty()) {
            T u = fila.poll();
            if (!finalizados.add(u)) continue;        // ja processado
            if (u.equals(destino)) break;             // chegamos

            double du = dist.get(u);
            for (GrafoPonderado.Aresta<T> a : grafo.arestas(u)) {
                if (finalizados.contains(a.destino)) continue;
                double nova = du + a.peso;
                if (nova < dist.getOrDefault(a.destino, Double.POSITIVE_INFINITY)) {
                    dist.put(a.destino, nova);
                    anterior.put(a.destino, u);
                    fila.add(a.destino);              // re-enfileira com nova prioridade
                }
            }
        }

        double total = dist.getOrDefault(destino, Double.POSITIVE_INFINITY);
        if (total == Double.POSITIVE_INFINITY) {
            return new Resultado<>(List.of(), total);
        }

        // reconstroi o caminho do destino ate a origem
        List<T> caminho = new ArrayList<>();
        T atual = destino;
        while (atual != null) {
            caminho.add(atual);
            atual = anterior.get(atual);
        }
        Collections.reverse(caminho);
        return new Resultado<>(caminho, total);
    }
}
