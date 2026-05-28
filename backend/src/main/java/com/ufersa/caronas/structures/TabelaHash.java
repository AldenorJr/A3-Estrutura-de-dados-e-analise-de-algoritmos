package com.ufersa.caronas.structures;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementacao propria de Tabela Hash com encadeamento separado (separate chaining).
 *
 * Decisao academica: NAO usamos java.util.HashMap. Implementamos do zero para
 * demonstrar dominio do algoritmo (criterio A3).
 *
 * Complexidade:
 *   put / get / remove
 *     - caso medio:  O(1)
 *     - pior caso:   O(n) (todas as chaves colidem)
 *
 * Estrategia de colisao: encadeamento separado (cada balde guarda uma lista).
 * Estrategia de redimensionamento: dobra a capacidade quando fator de carga > 0.75.
 *
 * @param <K> tipo da chave
 * @param <V> tipo do valor
 */
public class TabelaHash<K, V> {

    private static final int CAPACIDADE_INICIAL = 16;
    private static final double FATOR_CARGA_MAX = 0.75;

    private static class Entrada<K, V> {
        final K chave;
        V valor;
        Entrada<K, V> proximo;

        Entrada(K chave, V valor) {
            this.chave = chave;
            this.valor = valor;
        }
    }

    private Entrada<K, V>[] baldes;
    private int tamanho;

    @SuppressWarnings("unchecked")
    public TabelaHash() {
        this.baldes = (Entrada<K, V>[]) new Entrada[CAPACIDADE_INICIAL];
        this.tamanho = 0;
    }

    /**
     * Funcao de hash: pega o hashCode da chave, normaliza para positivo e
     * usa modulo pelo tamanho do array de baldes para escolher o indice.
     */
    private int indiceDoBalde(K chave, int capacidade) {
        int h = (chave == null) ? 0 : chave.hashCode();
        // Math.floorMod garante resultado positivo mesmo para hashCode negativo
        return Math.floorMod(h, capacidade);
    }

    /**
     * Insere ou atualiza um par chave-valor. O(1) medio.
     */
    public void put(K chave, V valor) {
        if ((double) (tamanho + 1) / baldes.length > FATOR_CARGA_MAX) {
            redimensionar();
        }
        int idx = indiceDoBalde(chave, baldes.length);
        Entrada<K, V> cabeca = baldes[idx];
        // procura se a chave ja existe no balde
        for (Entrada<K, V> e = cabeca; e != null; e = e.proximo) {
            if (igual(e.chave, chave)) {
                e.valor = valor;
                return;
            }
        }
        // chave nova: insere no inicio do balde (O(1))
        Entrada<K, V> nova = new Entrada<>(chave, valor);
        nova.proximo = cabeca;
        baldes[idx] = nova;
        tamanho++;
    }

    /**
     * Recupera o valor de uma chave. O(1) medio.
     * Retorna null se nao encontrar.
     */
    public V get(K chave) {
        int idx = indiceDoBalde(chave, baldes.length);
        for (Entrada<K, V> e = baldes[idx]; e != null; e = e.proximo) {
            if (igual(e.chave, chave)) return e.valor;
        }
        return null;
    }

    /**
     * Remove e retorna o valor associado a chave (ou null). O(1) medio.
     */
    public V remove(K chave) {
        int idx = indiceDoBalde(chave, baldes.length);
        Entrada<K, V> anterior = null;
        for (Entrada<K, V> e = baldes[idx]; e != null; e = e.proximo) {
            if (igual(e.chave, chave)) {
                if (anterior == null) {
                    baldes[idx] = e.proximo;
                } else {
                    anterior.proximo = e.proximo;
                }
                tamanho--;
                return e.valor;
            }
            anterior = e;
        }
        return null;
    }

    public boolean contemChave(K chave) {
        return get(chave) != null;
    }

    public int tamanho() {
        return tamanho;
    }

    public int capacidade() {
        return baldes.length;
    }

    /**
     * Retorna todos os valores armazenados. O(n).
     * Util para iteracao quando precisamos varrer toda a base.
     */
    public List<V> todosValores() {
        List<V> lista = new ArrayList<>(tamanho);
        for (Entrada<K, V> cabeca : baldes) {
            for (Entrada<K, V> e = cabeca; e != null; e = e.proximo) {
                lista.add(e.valor);
            }
        }
        return lista;
    }

    public List<K> todasChaves() {
        List<K> lista = new ArrayList<>(tamanho);
        for (Entrada<K, V> cabeca : baldes) {
            for (Entrada<K, V> e = cabeca; e != null; e = e.proximo) {
                lista.add(e.chave);
            }
        }
        return lista;
    }

    /**
     * Dobra a capacidade e reposiciona todas as entradas. O(n) amortizado.
     */
    @SuppressWarnings("unchecked")
    private void redimensionar() {
        int novaCapacidade = baldes.length * 2;
        Entrada<K, V>[] novos = (Entrada<K, V>[]) new Entrada[novaCapacidade];
        for (Entrada<K, V> cabeca : baldes) {
            for (Entrada<K, V> e = cabeca; e != null; ) {
                Entrada<K, V> prox = e.proximo;
                int idx = indiceDoBalde(e.chave, novaCapacidade);
                e.proximo = novos[idx];
                novos[idx] = e;
                e = prox;
            }
        }
        this.baldes = novos;
    }

    private boolean igual(K a, K b) {
        return (a == null) ? b == null : a.equals(b);
    }

    /**
     * Estatistica academica: quantos baldes estao ocupados.
     * Util para mostrar na apresentacao a qualidade da distribuicao.
     */
    public int baldesOcupados() {
        int count = 0;
        for (Entrada<K, V> b : baldes) if (b != null) count++;
        return count;
    }
}
