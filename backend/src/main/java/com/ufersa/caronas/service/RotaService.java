package com.ufersa.caronas.service;

import com.ufersa.caronas.model.Rota;
import com.ufersa.caronas.structures.TabelaHash;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Servico de rotas. Tambem usa nossa TabelaHash custom.
 *
 * Tabelas:
 *   1. porId             -> O(1)
 *   2. porBairroOrigem   -> O(1) para listar rotas saindo de um bairro
 *   3. porUsuarioId      -> O(1) para listar rotas de um usuario
 */
@Service
public class RotaService {

    private final TabelaHash<Long, Rota> porId = new TabelaHash<>();
    private final TabelaHash<String, List<Rota>> porBairroOrigem = new TabelaHash<>();
    private final TabelaHash<Long, List<Rota>> porUsuarioId = new TabelaHash<>();

    public Rota salvar(Rota r) {
        porId.put(r.getId(), r);

        String chaveBairro = normalizar(r.getBairroOrigem());
        List<Rota> bairroLista = porBairroOrigem.get(chaveBairro);
        if (bairroLista == null) {
            bairroLista = new ArrayList<>();
            porBairroOrigem.put(chaveBairro, bairroLista);
        }
        bairroLista.add(r);

        List<Rota> usuarioLista = porUsuarioId.get(r.getUsuarioId());
        if (usuarioLista == null) {
            usuarioLista = new ArrayList<>();
            porUsuarioId.put(r.getUsuarioId(), usuarioLista);
        }
        usuarioLista.add(r);

        return r;
    }

    public Rota buscarPorId(Long id) {
        return porId.get(id);
    }

    public List<Rota> buscarPorBairro(String bairro) {
        List<Rota> r = porBairroOrigem.get(normalizar(bairro));
        return r == null ? List.of() : new ArrayList<>(r);
    }

    public List<Rota> buscarPorUsuario(Long usuarioId) {
        List<Rota> r = porUsuarioId.get(usuarioId);
        return r == null ? List.of() : new ArrayList<>(r);
    }

    public List<Rota> listarTodas() {
        return porId.todosValores();
    }

    public int total() { return porId.tamanho(); }

    private String normalizar(String s) {
        return s == null ? "" : s.trim().toLowerCase();
    }
}
