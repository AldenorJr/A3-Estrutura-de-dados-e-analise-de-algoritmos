package com.ufersa.caronas.structures;

import java.util.ArrayList;
import java.util.List;

/**
 * Grafo NAO direcionado PONDERADO, em lista de adjacencia.
 *
 * No projeto, usamos para representar o "mapa de bairros":
 *   - cada bairro e um no
 *   - cada aresta tem como peso a distancia aproximada (km) entre eles
 *
 * E sobre este grafo que o algoritmo de Dijkstra opera para
 * encontrar o melhor trajeto entre dois bairros.
 *
 * Reutiliza nossa propria TabelaHash para indexar os nos.
 *
 * @param <T> tipo dos nos
 */
public class GrafoPonderado<T> {

    public static class Aresta<T> {
        public final T destino;
        public final double peso;
        public Aresta(T destino, double peso) {
            this.destino = destino;
            this.peso = peso;
        }
    }

    private final TabelaHash<T, List<Aresta<T>>> adjacencia;

    public GrafoPonderado() {
        this.adjacencia = new TabelaHash<>();
    }

    /** Adiciona um no isolado. O(1). */
    public void addNo(T no) {
        if (!adjacencia.contemChave(no)) {
            adjacencia.put(no, new ArrayList<>());
        }
    }

    /**
     * Adiciona aresta nao direcionada entre a e b com o peso informado.
     * Se uma aresta entre eles ja existir, e atualizada para o novo peso.
     */
    public void addAresta(T a, T b, double peso) {
        addNo(a);
        addNo(b);
        atualizarOuInserir(a, b, peso);
        atualizarOuInserir(b, a, peso);
    }

    private void atualizarOuInserir(T origem, T destino, double peso) {
        List<Aresta<T>> lista = adjacencia.get(origem);
        for (int i = 0; i < lista.size(); i++) {
            if (lista.get(i).destino.equals(destino)) {
                lista.set(i, new Aresta<>(destino, peso));
                return;
            }
        }
        lista.add(new Aresta<>(destino, peso));
    }

    public List<Aresta<T>> arestas(T no) {
        List<Aresta<T>> r = adjacencia.get(no);
        return r == null ? List.of() : r;
    }

    public List<T> nos() {
        return adjacencia.todasChaves();
    }

    public boolean contem(T no) { return adjacencia.contemChave(no); }

    public int totalNos() { return adjacencia.tamanho(); }
}
