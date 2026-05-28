# Algoritmos e Estruturas de Dados — Detalhamento Técnico

Este documento descreve cada um dos três algoritmos implementados no
projeto. Para cada um, mostramos:

1. **O problema** que ele resolve no nosso sistema.
2. **Como funciona**, passo a passo.
3. **Pseudocódigo** simplificado.
4. **Complexidade (Big-O)** e análise.
5. **Por que escolhemos** essa estrutura.
6. **Localização no código** (`backend/src/main/java/.../structures/`).

---

## 1. Tabela Hash (Hash Table com Encadeamento Separado)

### Problema

> "Dado o bairro `Nova Betânia`, retorne todos os usuários
> cadastrados nele em tempo constante."

Sem uma estrutura adequada, essa busca seria O(n) — varrer toda a
lista de usuários toda vez. Com hash, é O(1) médio.

### Como funciona

A ideia central é simples: a chave (`"Nova Betânia"`) passa por uma
**função de hash** que retorna um número inteiro. Esse número é
convertido em um índice de array. O valor (lista de usuários do
bairro) é armazenado naquele índice.

```
Chave: "Nova Betânia"
  ↓
hashCode() = 1853441097
  ↓
Math.floorMod(1853441097, 16) = 9
  ↓
baldes[9] → [usuarioA, usuarioB, usuarioC]
```

#### Colisões

Duas chaves diferentes podem cair no mesmo índice (colisão). Existem
várias estratégias para lidar com isso. Escolhemos
**encadeamento separado** (separate chaining):

- Cada posição do array é o **início de uma lista encadeada**.
- Em caso de colisão, o novo item é simplesmente adicionado à lista
  daquele balde.
- Para buscar, calcula-se o índice e percorre-se a lista até achar
  a chave certa.

#### Redimensionamento (resize)

Quando o **fator de carga** (`tamanho / capacidade`) ultrapassa
**0.75**, a capacidade é dobrada e todos os elementos são re-inseridos.
Isso mantém as listas curtas e a busca rápida.

### Pseudocódigo

```
função put(chave, valor):
    se (tamanho + 1) / capacidade > 0.75:
        redimensionar()

    idx = floorMod(chave.hashCode(), capacidade)
    para cada entrada e em baldes[idx]:
        se e.chave == chave:
            e.valor = valor
            retornar
    nova_entrada = (chave, valor)
    inserir nova_entrada no início de baldes[idx]
    tamanho++

função get(chave):
    idx = floorMod(chave.hashCode(), capacidade)
    para cada entrada e em baldes[idx]:
        se e.chave == chave:
            retornar e.valor
    retornar null
```

### Big-O

| Operação              | Caso médio | Pior caso |
|-----------------------|-----------:|----------:|
| `put`                 | **O(1)**   | O(n)      |
| `get`                 | **O(1)**   | O(n)      |
| `remove`              | **O(1)**   | O(n)      |
| `todosValores`        | O(n)       | O(n)      |
| `redimensionar`       | O(n)       | O(n)      |
| `put` amortizado      | O(1)       | O(n)      |

O pior caso ocorre quando **todas as chaves colidem no mesmo balde**
— extremamente raro com `hashCode()` bem distribuído. Na prática,
nossas estatísticas (endpoint `/api/usuarios/stats`) mostram que
com **9 usuários em 7 bairros** ocupamos **6 baldes diferentes**
(boa distribuição).

### Por que escolhemos

- **Velocidade:** a operação mais comum no sistema é "buscar
  usuários por bairro". Hash é a estrutura nativa para esse padrão.
- **Independência de dados ordenados:** não precisamos de ordem
  natural entre os bairros (BST seria overkill).
- **Aprendizado acadêmico:** implementar do zero (sem `HashMap`)
  mostra domínio da estrutura, conforme exigido pelo enunciado.

### Onde está

`backend/src/main/java/com/ufersa/caronas/structures/TabelaHash.java`

---

## 2. Grafo (Graph com Lista de Adjacência) + BFS

### Problema

> "Dado um usuário, encontre todos os outros que são compatíveis
> para carona — diretamente ou através de conexões transitivas."

Compatibilidade entre estudantes é uma **relação**, não um
atributo isolado. Grafos são a estrutura natural para representar
relações.

### Modelo do grafo

- **Nós:** usuários.
- **Arestas:** existe aresta entre A e B se:
  - A e B estudam na mesma universidade
  - moram no mesmo bairro ou em bairros vizinhos
  - têm pelo menos uma rota com horário dentro da janela aceitável

Como a relação é simétrica ("A é compatível com B" ⇔ "B é
compatível com A"), o grafo é **não direcionado**.

### Representação interna

Existem duas representações clássicas para grafos:

| Representação        | Memória   | Verificar aresta | Listar vizinhos |
|----------------------|-----------|------------------|-----------------|
| Matriz de adjacência | O(V²)     | O(1)             | O(V)            |
| **Lista de adjacência** | **O(V+E)** | O(grau)        | **O(grau)**     |

Escolhemos **lista de adjacência** porque o grafo é **esparso**
(cada usuário conecta com poucos outros, não com todos). A matriz
gastaria memória demais.

Internamente, a lista de adjacência é uma `TabelaHash<T, List<T>>` —
ou seja, a própria estrutura Hash é reutilizada aqui.

### BFS (Busca em Largura)

A partir do solicitante, queremos descobrir **todos os usuários
alcançáveis** (componente conectado). Usamos BFS:

```
função BFS(origem):
    visitados = conjunto vazio
    fila = nova fila contendo [origem]
    visitados.adicionar(origem)
    resultado = []

    enquanto fila não está vazia:
        atual = fila.remove()
        resultado.adicionar(atual)
        para cada vizinho de atual:
            se vizinho não está em visitados:
                visitados.adicionar(vizinho)
                fila.adicionar(vizinho)

    retornar resultado
```

### Big-O

| Operação           | Complexidade |
|--------------------|-------------:|
| `addNo`            | O(1)         |
| `addAresta(a, b)`  | O(grau(a) + grau(b)) |
| `vizinhos(n)`      | O(grau(n))   |
| **BFS**            | **O(V + E)** |
| `totalArestas`     | O(V + E)     |

### Por que escolhemos

- **Naturalidade:** compatibilidade entre estudantes é uma relação,
  e a estrutura natural para relações é grafo.
- **Eficiência em busca transitiva:** BFS encontra "amigos de
  amigos" (clusters) em O(V + E).
- **Demonstração acadêmica:** mostra uso real de uma estrutura
  não-trivial.

### Onde está

`backend/src/main/java/com/ufersa/caronas/structures/Grafo.java`

---

## 3. QuickSort

### Problema

> "Ordene a lista de matches por score decrescente, para que o
> usuário veja o melhor primeiro."

### Como funciona

QuickSort é um algoritmo de **divisão e conquista**:

1. **Escolha um pivô** (usamos o elemento do meio para evitar
   pior caso em listas já ordenadas).
2. **Particione**: reorganize a lista para que tudo ≤ pivô
   fique à esquerda dele, e tudo > pivô fique à direita.
3. **Recurse** nas duas metades.

### Pseudocódigo

```
função quicksort(lista, inicio, fim, comparador):
    se inicio >= fim:
        retornar
    indice_pivo = particionar(lista, inicio, fim, comparador)
    quicksort(lista, inicio, indice_pivo - 1, comparador)
    quicksort(lista, indice_pivo + 1, fim, comparador)

função particionar(lista, inicio, fim, cmp):
    meio = inicio + (fim - inicio) / 2
    pivo = lista[meio]
    troca(lista, meio, fim)            # move pivô para o final

    i = inicio - 1
    para j de inicio até fim - 1:
        se cmp(lista[j], pivo) <= 0:
            i++
            troca(lista, i, j)
    troca(lista, i + 1, fim)            # coloca pivô na posição final
    retornar i + 1
```

### Big-O

| Cenário        | Complexidade |
|----------------|-------------:|
| Melhor caso    | O(n log n)   |
| **Caso médio** | **O(n log n)** |
| Pior caso      | O(n²) — raro com pivô do meio |
| Memória (pilha de recursão) | O(log n) |

### Comparação com alternativas

| Algoritmo      | Médio       | Pior       | Memória | Estável |
|----------------|-------------|------------|---------|---------|
| Bubble Sort    | O(n²)       | O(n²)      | O(1)    | sim     |
| Selection Sort | O(n²)       | O(n²)      | O(1)    | não     |
| Insertion Sort | O(n²)       | O(n²)      | O(1)    | sim     |
| **QuickSort**  | **O(n log n)** | O(n²) | O(log n) | não  |
| Merge Sort     | O(n log n)  | O(n log n) | O(n)    | sim     |
| Heap Sort      | O(n log n)  | O(n log n) | O(1)    | não     |

QuickSort é o algoritmo de ordenação **mais usado na prática** porque:

- Tem o melhor desempenho médio entre os comparativos.
- Usa pouca memória adicional (O(log n) na pilha).
- A constante escondida do Big-O é pequena (poucas operações por
  elemento).

A escolha do **pivô do meio** (em vez do primeiro ou último) reduz
drasticamente a chance de cair no pior caso O(n²).

### Por que escolhemos

- O número de matches pode crescer (digamos, 50–200 caronas
  candidatas em horários de pico). Bubble Sort sofreria;
  QuickSort entrega resultado instantâneo.
- Implementação acadêmica clara, mostrando divisão e conquista.

### Onde está

`backend/src/main/java/com/ufersa/caronas/structures/QuickSort.java`

---

## 4. Dijkstra (Caminho Mínimo em Grafo Ponderado)

### Problema

> "Dado um motorista no bairro X, um passageiro no bairro Y, e o
> destino UFERSA — qual o melhor trajeto (menor distância) passando
> por bairros vizinhos?"

### Como funciona

Dijkstra é o algoritmo clássico para **menor caminho** em grafos com
pesos não-negativos. Ele mantém um conjunto de bairros "finalizados"
(com distância mínima conhecida) e expande progressivamente para os
vizinhos com menor distância acumulada.

### Estrutura sobre a qual opera

Criamos uma classe `GrafoPonderado<T>` (também própria,
em `structures/GrafoPonderado.java`). Diferente do `Grafo<T>` usado
para compatibilidade entre usuários (sem peso), aqui as arestas têm
um peso — no nosso caso, a **distância em km entre dois bairros**,
calculada via fórmula de Haversine sobre as coordenadas geográficas
reais de Mossoró/RN.

### Pseudocódigo

```
função dijkstra(grafo, origem, destino):
    dist[v] = infinito para todo v
    dist[origem] = 0
    anterior[v] = null para todo v
    heap = min-heap por dist[v]
    heap.add(origem)

    enquanto heap não está vazia:
        u = heap.remove()                    # bairro com menor dist atual
        se u já finalizado: continue
        finaliza(u)
        se u == destino: pare

        para cada aresta (u → v, peso) em grafo.arestas(u):
            nova = dist[u] + peso
            se nova < dist[v]:
                dist[v] = nova
                anterior[v] = u
                heap.add(v)                  # re-enfileira com prioridade nova

    # reconstroi o caminho de destino até origem usando anterior[]
    caminho = []
    nó = destino
    enquanto nó != null:
        caminho.adiciona(nó)
        nó = anterior[nó]
    retornar reverse(caminho), dist[destino]
```

### Big-O

| Implementação                              | Complexidade        |
|--------------------------------------------|---------------------|
| Sem heap (busca linear)                    | O(V²)               |
| **Com heap binário (PriorityQueue)**       | **O((V + E) log V)** |
| Com heap de Fibonacci                      | O(E + V log V)      |

Usamos a versão com **heap binário** (a classe `PriorityQueue` do
JDK é usada apenas como min-heap genérico — o algoritmo em si é
nosso). Para nossa malha de 14 bairros e ~22 arestas, o cálculo é
instantâneo.

### Por que escolhemos

- **Foco em distância real:** BFS comum acharia o caminho com **menos
  bairros**, mas Dijkstra acha o caminho com **menos quilômetros** —
  é o que importa para o usuário.
- **Pesos não-negativos:** distâncias geográficas nunca são negativas,
  então Dijkstra é a escolha correta (vs. Bellman-Ford, que aceita
  pesos negativos com custo maior).
- **Aplicação clássica:** todo aplicativo de mapas (Google Maps, Waze,
  Uber) usa variantes de Dijkstra/A*. Mostrar isso na A3 conecta a
  teoria à prática profissional.

### Aplicação no sistema

Quando o usuário clica em **"Ver trajeto no mapa"** sobre um match:

1. O backend executa **dois Dijkstras**:
   - Perna 1: bairro do motorista → bairro do passageiro
   - Perna 2: bairro do passageiro → UFERSA
2. Junta os dois caminhos (sem duplicar o ponto de pickup).
3. Calcula distância total (Haversine de cada segmento) e
   tempo estimado (30 km/h área urbana).
4. Retorna ao frontend a sequência de bairros + coordenadas + métricas.
5. O frontend desenha no **mapa Leaflet** com tiles OpenStreetMap;
   tenta enriquecer com **rota real nas ruas via OSRM** (e cai para
   linhas retas se OSRM estiver indisponível).

### Onde está

- `backend/src/main/java/com/ufersa/caronas/structures/Dijkstra.java`
- `backend/src/main/java/com/ufersa/caronas/structures/GrafoPonderado.java`

---

## 5. Como os quatro se integram

Pipeline completo de uma busca de carona:

```
USUÁRIO clica "Buscar"
        │
        ▼
┌───────────────────────────────────────────────────────────┐
│  ETAPA 1 — HASH (O(1) por lookup)                         │
│  Para cada bairro vizinho do solicitante,                 │
│  buscar todas as rotas cadastradas naquele bairro.        │
│  Resultado: conjunto C de candidatos.                     │
└───────────────────────────────────────────────────────────┘
        │
        ▼
┌───────────────────────────────────────────────────────────┐
│  ETAPA 2 — GRAFO + BFS (O(V + E))                         │
│  Construir grafo de compatibilidade usando C.             │
│  BFS partindo do solicitante → cluster alcançável A.      │
│  Resultado: A ⊆ C, filtrando incompatibilidades.          │
└───────────────────────────────────────────────────────────┘
        │
        ▼
┌───────────────────────────────────────────────────────────┐
│  ETAPA 3 — Filtros adicionais e cálculo de SCORE (O(k))   │
│  Para cada candidato em A:                                │
│    - mesma universidade?                                  │
│    - horário dentro da janela?                            │
│    - vagas disponíveis?                                   │
│    - é motorista válido?                                  │
│  Se passa, calcula score = avaliação×40                   │
│                            + proximidade_bairro×30        │
│                            + compatibilidade_horario×25   │
│                            + vagas_normalizadas×5         │
└───────────────────────────────────────────────────────────┘
        │
        ▼
┌───────────────────────────────────────────────────────────┐
│  ETAPA 4 — QUICKSORT (O(k log k))                         │
│  Ordena a lista de matches por score decrescente.         │
└───────────────────────────────────────────────────────────┘
        │
        ▼
   JSON ordenado → frontend → usuário vê os melhores primeiro.
```

**Complexidade total:** O(V + E + k log k)

onde V = usuários no cluster, E = arestas de compatibilidade, k =
matches finais. Em todas as condições realistas, isso é **vários
ordens de magnitude mais rápido** do que a abordagem ingênua de
"comparar todos com todos e ordenar com bubble sort"
(O(n² + n²)) = O(n²).
