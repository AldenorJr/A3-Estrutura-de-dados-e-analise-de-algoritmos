# Arquitetura do Sistema

## Visão geral

```
┌────────────────────────────────────────────────────────────────┐
│                          NAVEGADOR                              │
│                                                                 │
│   ┌─────────────────────────────────────────────────────────┐  │
│   │   frontend/index.html (HTML + CSS + JS puro)            │  │
│   │   - Formulários de cadastro                             │  │
│   │   - Tela de busca de carona                             │  │
│   │   - Cards de resultado com score                        │  │
│   └────────────────────────┬────────────────────────────────┘  │
└────────────────────────────│────────────────────────────────────┘
                             │   HTTP/JSON (CORS habilitado)
                             ▼
┌────────────────────────────────────────────────────────────────┐
│                  BACKEND — Spring Boot (Java 17)                │
│                  http://localhost:8080                          │
│                                                                 │
│   ┌─────────── controller (REST) ──────────────────────────┐   │
│   │  UsuarioController · RotaController                    │   │
│   │  MatchController · BairroController                    │   │
│   └──────────────────────┬─────────────────────────────────┘   │
│                          ▼                                      │
│   ┌─────────── service (regras de negócio) ────────────────┐   │
│   │  UsuarioService  ──┐                                   │   │
│   │  RotaService     ──┼─→ usa TabelaHash                  │   │
│   │  BairroService   ──┘                                   │   │
│   │  MatchService    ─→ usa Hash + Grafo + QuickSort       │   │
│   └──────────────────────┬─────────────────────────────────┘   │
│                          ▼                                      │
│   ┌─────────── structures (★ algoritmos do projeto) ──────┐    │
│   │  TabelaHash<K,V>   — encadeamento separado            │    │
│   │  Grafo<T>          — lista de adjacência + BFS        │    │
│   │  QuickSort         — ordenação genérica               │    │
│   └────────────────────────────────────────────────────────┘   │
│                                                                 │
│   ┌─────────── model (POJOs) ──────────────────────────────┐   │
│   │  Usuario · Rota · Veiculo · MatchResult · TipoRota     │   │
│   └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│   ┌─────────── seed ───────────────────────────────────────┐   │
│   │  SeedDataRunner — carrega 9 usuários + 8 rotas         │   │
│   │  na inicialização                                      │   │
│   └─────────────────────────────────────────────────────────┘   │
└────────────────────────────────────────────────────────────────┘
```

## Camadas

### Controller (REST)

Expõe endpoints HTTP. Cada controller é fino — só converte
`HttpRequest` em chamada de service e devolve JSON.

### Service (regras de negócio)

Onde mora a lógica. O `MatchService` é o mais importante: orquestra
Hash + Grafo + QuickSort para responder à busca de caronas.

### Structures (★ coração do projeto)

Implementações próprias das três estruturas de dados. Não dependem
de Spring — são puras (POJO) e poderiam ser reusadas em qualquer
projeto Java.

### Model (domínio)

POJOs simples (`Usuario`, `Rota`, `Veiculo`, `MatchResult`).
Sem anotações de persistência — armazenamos tudo em memória nas
tabelas hash.

### Seed

Roda na inicialização (via `CommandLineRunner`) e popula dados
de exemplo para demonstração.

## Fluxo de uma requisição típica

### POST /api/usuarios

```
Frontend → UsuarioController.criar(dto)
              → new Usuario(...)
              → usuarioService.salvar(u)
                    → porId.put(u.id, u)          [hash]
                    → porBairro.get(bairro)       [hash]
                    → lista.add(u)
              → return Usuario JSON
```

### GET /api/match?usuarioId=6&horario=07:15

```
Frontend → MatchController.buscar(6, "07:15", null)
              → MatchService.buscarCaronas(...)
                  1. solicitante = usuarioService.buscarPorId(6)        [hash]
                  2. candidatos = rotaService.buscarPorBairro(bairro
                                + bairros vizinhos)                      [hash]
                  3. grafo = construirGrafo(solicitante, candidatos)
                     alcancaveis = grafo.buscaEmLargura(solicitante)     [BFS]
                  4. para cada rota em candidatos:
                        se passa filtros: calcula score
                                          adiciona em matches
                  5. QuickSort.ordenar(matches, comparador_score_desc)   [quicksort]
              → List<MatchResult> JSON
```

## Decisões de design

### Por que não usar HashMap do Java?

Porque o objetivo acadêmico do projeto é demonstrar **implementação**
das estruturas de dados, não apenas seu **uso**. Usar `HashMap` seria
o mesmo que copiar a resposta da prova.

Por isso, `TabelaHash`, `Grafo` e `QuickSort` foram implementados do
zero. As demais classes do JDK (`ArrayList`, `LinkedList`, `Queue`,
`HashSet`) são usadas como utilitários internos, mas nunca para
substituir as estruturas-foco do projeto.

### Por que armazenamento em memória, sem banco de dados?

- O foco da disciplina é estruturas de dados, não persistência.
- Manter tudo em memória usando a nossa própria Hash *demonstra* o
  uso real da estrutura — é a "vida real" delas.
- Simplifica a execução: o avaliador roda `mvn spring-boot:run` e
  já tem o sistema funcionando.

### Por que separar backend e frontend?

- Demonstra separação de responsabilidades (camada de apresentação
  vs. camada de dados/lógica).
- Permite que o backend sirva qualquer cliente (web, mobile, CLI).
- Facilita testes manuais via `curl` (vide README).

### Por que CORS aberto?

Apenas para o cenário acadêmico. Em produção, o `addCorsMappings`
seria restrito a domínios específicos.

## Diagrama de classes (resumido)

```
┌──────────────────┐         ┌──────────────────┐
│     Usuario      │         │      Rota        │
├──────────────────┤         ├──────────────────┤
│ - id             │         │ - id             │
│ - nome           │         │ - usuarioId      │
│ - email          │         │ - bairroOrigem   │
│ - curso          │         │ - destino        │
│ - bairro         │         │ - horarioSaida   │
│ - universidade   │         │ - vagasDisponiveis│
│ - motorista      │         │ - tipo (IDA/VOLTA)│
│ - avaliacao      │         └──────────────────┘
│ - veiculo*       │                  ▲
└────────┬─────────┘                  │ N
         │ 1                          │
         └──── owns ────► Veiculo     │
                                      │
              ┌───────────────────────┘
              │
     ┌────────┴───────────┐
     │   MatchResult      │
     ├────────────────────┤
     │ - motorista        │
     │ - rota             │
     │ - score (0..100)   │
     │ - diferencaMinutos │
     │ - compatibilidade  │
     └────────────────────┘
```
