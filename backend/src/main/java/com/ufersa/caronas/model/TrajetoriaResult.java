package com.ufersa.caronas.model;

import java.util.List;

/**
 * Resultado da consulta de melhor trajeto entre motorista, passageiro e destino.
 * Estruturado para alimentar diretamente o mapa do frontend.
 */
public class TrajetoriaResult {

    public static class Parada {
        public String bairro;
        public String tipo;        // PARTIDA, PICKUP, DESTINO ou ROTA
        public String descricao;
        public Coordenada coordenada;

        public Parada() {}
        public Parada(String bairro, String tipo, String descricao, Coordenada c) {
            this.bairro = bairro; this.tipo = tipo;
            this.descricao = descricao; this.coordenada = c;
        }
    }

    public List<Parada> paradas;            // sequencia ordenada (origem motorista -> pickup -> destino)
    public List<Coordenada> caminhoPoligono; // todas as coordenadas do trajeto (para desenhar linha no mapa)
    public List<String> bairrosCaminho;     // sequencia de bairros calculada por Dijkstra
    public double distanciaTotalKm;
    public int tempoEstimadoMin;            // assumindo 30 km/h em area urbana
    public double economiaEstimadaReais;    // economia para o passageiro vs onibus/uber
    public String algoritmoUsado;           // "Dijkstra"
}
