package com.ufersa.caronas.structures;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * Grafo NAO direcionado implementado com lista de adjacencia.
 *
 * No contexto do sistema de caronas:
 *  - Nos:    usuarios
 *  - Arestas: existe aresta entre A e B se ha compatibilidade de carona
 *             (mesma universidade + bairros proximos + horarios proximos).
 *
 * Decisao academica: NAO usamos uma biblioteca de grafos pronta. Implementamos
 * lista de adjacencia do zero (mais eficiente em memoria que matriz para grafos
 * esparsos como este).
 *
 * Complexidade:
 *   addNo            -> O(1)
 *   addAresta        -> O(grau medio) ~ O(1) na pratica
 *   vizinhos(n)      -> O(grau de n)
 *   buscaEmLargura   -> O(V + E)
 *
 * @param <T> tipo do no (Usuario, no nosso caso)
 */
public class Grafo<T> {

    /** Estrutura interna: cada no aponta para uma lista de vizinhos. */
    private final TabelaHash<T, List<T>> adjacencia;

    public Grafo() {
        this.adjacencia = new TabelaHash<>();
    }

    /** Adiciona um no isolado (sem arestas). O(1). */
    public void addNo(T no) {
        if (!adjacencia.contemChave(no)) {
            adjacencia.put(no, new ArrayList<>());
        }
    }

    /**
     * Adiciona aresta nao direcionada entre a e b.
     * Se um dos nos nao existe, e adicionado automaticamente.
     */
    public void addAresta(T a, T b) {
        addNo(a);
        addNo(b);
        List<T> vizA = adjacencia.get(a);
        List<T> vizB = adjacencia.get(b);
        if (!vizA.contains(b)) vizA.add(b);
        if (!vizB.contains(a)) vizB.add(a);
    }

    /** Vizinhos diretos de um no. O(1) para acesso + O(grau) para a lista. */
    public List<T> vizinhos(T no) {
        List<T> v = adjacencia.get(no);
        return v == null ? List.of() : new ArrayList<>(v);
    }

    /** Retorna todos os nos do grafo. */
    public List<T> nos() {
        return adjacencia.todasChaves();
    }

    public int totalNos() { return adjacencia.tamanho(); }

    public int totalArestas() {
        int soma = 0;
        for (T no : adjacencia.todasChaves()) {
            soma += adjacencia.get(no).size();
        }
        return soma / 2; // cada aresta foi contada duas vezes
    }

    /**
     * Busca em Largura (BFS) a partir de um no.
     * Retorna todos os nos alcancaveis (componente conectado).
     * Util para descobrir "grupos de carona" - clusters de pessoas compatíveis.
     *
     * Complexidade: O(V + E)
     */
    public List<T> buscaEmLargura(T origem) {
        List<T> ordem = new ArrayList<>();
        if (!adjacencia.contemChave(origem)) return ordem;

        Set<T> visitados = new HashSet<>();
        Queue<T> fila = new LinkedList<>();
        fila.add(origem);
        visitados.add(origem);

        while (!fila.isEmpty()) {
            T atual = fila.poll();
            ordem.add(atual);
            for (T vizinho : adjacencia.get(atual)) {
                if (!visitados.contains(vizinho)) {
                    visitados.add(vizinho);
                    fila.add(vizinho);
                }
            }
        }
        return ordem;
    }

    /**
     * Remove todas as arestas (mantem os nos). Util para reconstrucao quando
     * usuarios/rotas mudam.
     */
    public void limparArestas() {
        for (T no : adjacencia.todasChaves()) {
            adjacencia.get(no).clear();
        }
    }
}
