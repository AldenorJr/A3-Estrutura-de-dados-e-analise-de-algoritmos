package com.ufersa.caronas.service;

import com.ufersa.caronas.model.Usuario;
import com.ufersa.caronas.structures.TabelaHash;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Servico de usuarios. Usa nossa TabelaHash custom para armazenamento.
 *
 * Mantemos DUAS tabelas hash:
 *   1. porId     -> busca direta por ID (O(1))
 *   2. porBairro -> busca por bairro retorna lista de usuarios daquele bairro (O(1))
 *
 * Esta escolha demonstra o uso de hash para acelerar QUERIES diferentes.
 */
@Service
public class UsuarioService {

    private final TabelaHash<Long, Usuario> porId = new TabelaHash<>();
    private final TabelaHash<String, List<Usuario>> porBairro = new TabelaHash<>();

    public Usuario salvar(Usuario u) {
        porId.put(u.getId(), u);
        // adiciona no indice por bairro
        String bairro = normalizar(u.getBairro());
        List<Usuario> lista = porBairro.get(bairro);
        if (lista == null) {
            lista = new ArrayList<>();
            porBairro.put(bairro, lista);
        }
        // evita duplicata
        boolean existe = false;
        for (Usuario existente : lista) {
            if (existente.getId().equals(u.getId())) {
                existe = true;
                break;
            }
        }
        if (!existe) lista.add(u);
        return u;
    }

    /**
     * Atualiza um usuario ja existente. Se o bairro mudou, re-indexa na
     * TabelaHash por bairro (remove do balde antigo, adiciona no novo).
     */
    public Usuario atualizar(Usuario u, String bairroAntigo) {
        porId.put(u.getId(), u); // mesma referencia; mantem o indice por id
        String antigo = normalizar(bairroAntigo);
        String novo = normalizar(u.getBairro());
        if (!antigo.equals(novo)) {
            List<Usuario> listaAntiga = porBairro.get(antigo);
            if (listaAntiga != null) listaAntiga.removeIf(x -> x.getId().equals(u.getId()));
            List<Usuario> listaNova = porBairro.get(novo);
            if (listaNova == null) {
                listaNova = new ArrayList<>();
                porBairro.put(novo, listaNova);
            }
            boolean existe = false;
            for (Usuario existente : listaNova) {
                if (existente.getId().equals(u.getId())) { existe = true; break; }
            }
            if (!existe) listaNova.add(u);
        }
        return u;
    }

    public Usuario buscarPorId(Long id) {
        return porId.get(id);
    }

    public List<Usuario> buscarPorBairro(String bairro) {
        List<Usuario> r = porBairro.get(normalizar(bairro));
        return r == null ? List.of() : new ArrayList<>(r);
    }

    public List<Usuario> listarTodos() {
        return porId.todosValores();
    }

    public List<Usuario> listarMotoristas() {
        List<Usuario> motoristas = new ArrayList<>();
        for (Usuario u : porId.todosValores()) {
            if (u.isMotorista()) motoristas.add(u);
        }
        return motoristas;
    }

    public int total() {
        return porId.tamanho();
    }

    public int totalBairros() {
        return porBairro.tamanho();
    }

    public int baldesUsadosBairro() {
        return porBairro.baldesOcupados();
    }

    private String normalizar(String s) {
        return s == null ? "" : s.trim().toLowerCase();
    }
}
