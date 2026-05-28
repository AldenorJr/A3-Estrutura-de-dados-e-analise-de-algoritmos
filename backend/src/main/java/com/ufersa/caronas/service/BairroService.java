package com.ufersa.caronas.service;

import com.ufersa.caronas.model.Coordenada;
import com.ufersa.caronas.structures.Dijkstra;
import com.ufersa.caronas.structures.GrafoPonderado;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Servico do "mapa de Mossoro":
 *   - guarda coordenadas (lat/lng) aproximadas de cada bairro
 *   - mantem um GrafoPonderado de proximidade (vizinhanca + distancia em km)
 *   - oferece API de caminho minimo via Dijkstra
 *
 * As coordenadas sao aproximadas para fins didaticos.
 */
@Service
public class BairroService {

    /** Universidade padrao (Mossoro/RN). Usada como default quando rota nao tem destino. */
    public static final String DESTINO_UFERSA = "UFERSA";

    /** Universidades suportadas (sao tratadas como "nos" do grafo de bairros). */
    public static final Set<String> UNIVERSIDADES = new java.util.LinkedHashSet<>(
            java.util.Arrays.asList("UFERSA", "UERN", "IFRN"));

    private final Map<String, Coordenada> coordenadas = new LinkedHashMap<>();
    private final Map<String, Set<String>> bairrosProximos = new HashMap<>();
    private final GrafoPonderado<String> mapa = new GrafoPonderado<>();

    @PostConstruct
    void init() {
        // ===== Coordenadas aproximadas (Mossoro/RN) =====
        coord("Centro",                  -5.1875, -37.3441);
        coord("Nova Betania",            -5.1969, -37.3517);
        coord("Bom Jardim",              -5.1872, -37.3667);
        coord("Aeroporto",               -5.2017, -37.3645);
        coord("Alto de Sao Manoel",      -5.1981, -37.3289);
        coord("Doze Anos",               -5.1939, -37.3258);
        coord("Abolicao",                -5.1942, -37.3194);
        coord("Belo Horizonte",          -5.2058, -37.3611);
        coord("Santo Antonio",           -5.2106, -37.3725);
        coord("Costa e Silva",           -5.2106, -37.3306);
        coord("Presidente Costa e Silva",-5.2147, -37.3392);
        coord("Boa Vista",               -5.1933, -37.3056);
        coord("Bom Jesus",               -5.1825, -37.3056);

        // ===== Universidades (destinos) =====
        coord("UFERSA",                  -5.2030, -37.3261);  // Costa e Silva
        coord("UERN",                    -5.1953, -37.3473);  // Costa e Silva / Centro
        coord("IFRN",                    -5.1909, -37.3522);  // Nova Betania (Campus Mossoro)

        // ===== Vizinhanca bairro-a-bairro =====
        conectar("Centro", "Nova Betania");
        conectar("Centro", "Alto de Sao Manoel");
        conectar("Centro", "Doze Anos");
        conectar("Nova Betania", "Bom Jardim");
        conectar("Nova Betania", "Belo Horizonte");
        conectar("Bom Jardim", "Aeroporto");
        conectar("Bom Jardim", "Santo Antonio");
        conectar("Aeroporto", "Costa e Silva");
        conectar("Alto de Sao Manoel", "Doze Anos");
        conectar("Alto de Sao Manoel", "Abolicao");
        conectar("Abolicao", "Doze Anos");
        conectar("Abolicao", "Boa Vista");
        conectar("Belo Horizonte", "Santo Antonio");
        conectar("Santo Antonio", "Presidente Costa e Silva");
        conectar("Costa e Silva", "Presidente Costa e Silva");
        conectar("Boa Vista", "Bom Jesus");
        conectar("Boa Vista", "Abolicao");

        // ===== Arestas para UFERSA =====
        conectar("Aeroporto", "UFERSA");
        conectar("Costa e Silva", "UFERSA");
        conectar("Presidente Costa e Silva", "UFERSA");
        conectar("Doze Anos", "UFERSA");
        conectar("Alto de Sao Manoel", "UFERSA");

        // ===== Arestas para UERN =====
        conectar("Centro", "UERN");
        conectar("Costa e Silva", "UERN");
        conectar("Nova Betania", "UERN");
        conectar("Alto de Sao Manoel", "UERN");

        // ===== Arestas para IFRN =====
        conectar("Nova Betania", "IFRN");
        conectar("Bom Jardim", "IFRN");
        conectar("Centro", "IFRN");
        conectar("Belo Horizonte", "IFRN");
    }

    private void coord(String bairro, double lat, double lng) {
        coordenadas.put(bairro, new Coordenada(lat, lng));
    }

    private void conectar(String a, String b) {
        Coordenada ca = coordenadas.get(a);
        Coordenada cb = coordenadas.get(b);
        if (ca == null || cb == null) return;
        double km = ca.distanciaKm(cb);
        mapa.addAresta(a, b, km);

        bairrosProximos.computeIfAbsent(a, k -> new HashSet<>()).add(b);
        bairrosProximos.computeIfAbsent(b, k -> new HashSet<>()).add(a);
    }

    public boolean saoProximos(String a, String b) {
        if (a == null || b == null) return false;
        if (a.equalsIgnoreCase(b)) return true;
        Set<String> vizA = bairrosProximos.get(a);
        return vizA != null && vizA.contains(b);
    }

    public int distancia(String a, String b) {
        if (a == null || b == null) return 99;
        if (a.equalsIgnoreCase(b)) return 0;
        return saoProximos(a, b) ? 1 : 2;
    }

    public Set<String> todosBairros() {
        // exclui universidades da lista que o usuario escolhe como bairro residencial
        Set<String> apenasBairros = new java.util.LinkedHashSet<>(coordenadas.keySet());
        apenasBairros.removeAll(UNIVERSIDADES);
        return apenasBairros;
    }

    public Set<String> universidades() {
        return new java.util.LinkedHashSet<>(UNIVERSIDADES);
    }

    public Coordenada coordenadaDe(String bairro) {
        return coordenadas.get(bairro);
    }

    public Map<String, Coordenada> todasCoordenadas() {
        return new LinkedHashMap<>(coordenadas);
    }

    /**
     * Calcula o melhor trajeto (sequencia de bairros + distancia total)
     * entre dois bairros usando Dijkstra.
     */
    public Dijkstra.Resultado<String> melhorTrajeto(String origem, String destino) {
        return Dijkstra.caminhoMinimo(mapa, origem, destino);
    }
}
