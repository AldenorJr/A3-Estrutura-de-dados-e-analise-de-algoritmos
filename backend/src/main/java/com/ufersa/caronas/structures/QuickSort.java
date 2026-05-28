package com.ufersa.caronas.structures;

import java.util.Comparator;
import java.util.List;

/**
 * Implementacao propria do QuickSort (algoritmo de ordenacao por divisao e conquista).
 *
 * Decisao academica: NAO usamos List.sort() / Collections.sort(). Implementamos
 * QuickSort do zero para demonstrar o algoritmo (criterio A3).
 *
 * Como funciona (etapas):
 *  1. Escolhe um elemento como "pivo" (usamos o do meio para evitar pior caso
 *     em listas ja ordenadas).
 *  2. Particiona: coloca menores que o pivo a esquerda, maiores a direita.
 *  3. Aplica recursivamente nas duas metades.
 *
 * Complexidade:
 *   - Caso medio: O(n log n)
 *   - Pior caso:  O(n^2) (raro com pivo do meio em dados nao patologicos)
 *   - Memoria:    O(log n) na pilha de recursao
 *
 * @param <T> tipo dos elementos
 */
public final class QuickSort {

    private QuickSort() {}

    /** Ordena a lista in-place usando o Comparator fornecido. */
    public static <T> void ordenar(List<T> lista, Comparator<? super T> comparator) {
        if (lista == null || lista.size() < 2) return;
        quicksort(lista, 0, lista.size() - 1, comparator);
    }

    private static <T> void quicksort(List<T> lista, int inicio, int fim,
                                      Comparator<? super T> cmp) {
        if (inicio >= fim) return;
        int indicePivo = particionar(lista, inicio, fim, cmp);
        quicksort(lista, inicio, indicePivo - 1, cmp);
        quicksort(lista, indicePivo + 1, fim, cmp);
    }

    /**
     * Esquema de particionamento de Lomuto adaptado.
     * Escolhe pivo do meio (mais robusto que pivo do fim para listas
     * parcialmente ordenadas).
     */
    private static <T> int particionar(List<T> lista, int inicio, int fim,
                                       Comparator<? super T> cmp) {
        int meio = inicio + (fim - inicio) / 2;
        T pivo = lista.get(meio);
        // move o pivo para o fim
        trocar(lista, meio, fim);

        int i = inicio - 1;
        for (int j = inicio; j < fim; j++) {
            if (cmp.compare(lista.get(j), pivo) <= 0) {
                i++;
                trocar(lista, i, j);
            }
        }
        trocar(lista, i + 1, fim);
        return i + 1;
    }

    private static <T> void trocar(List<T> lista, int a, int b) {
        if (a == b) return;
        T tmp = lista.get(a);
        lista.set(a, lista.get(b));
        lista.set(b, tmp);
    }
}
