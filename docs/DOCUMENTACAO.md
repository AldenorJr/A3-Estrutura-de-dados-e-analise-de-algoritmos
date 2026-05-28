# DOCUMENTAÇÃO DO PROJETO — A3

**Disciplina:** Estruturas de Dados e Análise de Algoritmos
**Avaliação:** A3
**Tema:** Sistema de Match para Caronas Universitárias
**Universidade:** UFERSA — Universidade Federal Rural do Semi-Árido

---

Este documento atende, em ordem, aos **4 critérios de avaliação** definidos
no enunciado da A3.

---

## 1. Identificação do Problema

### O contexto

Milhares de estudantes da UFERSA (e de qualquer universidade
brasileira) enfrentam, diariamente, o mesmo desafio: **chegar à
universidade**. Em Mossoró/RN, o problema é particularmente sentido
porque:

- Boa parte dos alunos mora em bairros distantes do campus
  (Centro, Nova Betânia, Bom Jardim, Alto de São Manoel etc.).
- O transporte público é insuficiente e tem horários ruins.
- Combustível pesa no bolso de quem tem carro.
- Universitários geralmente têm horários parecidos
  (entradas às 7h, 13h, 18h).

### Como o problema é “resolvido” hoje

Hoje, a coordenação informal acontece em **grupos de WhatsApp**.
Esses grupos sofrem de problemas estruturais:

| Problema                                            | Consequência                          |
|-----------------------------------------------------|---------------------------------------|
| Mensagens se perdem no fluxo                        | Aluno não encontra a oferta a tempo   |
| Não há filtragem por bairro, horário ou universidade| Ruído enorme                          |
| Não há registro estruturado de disponibilidade      | Mesma pergunta repetida 10×           |
| Não há avaliação dos motoristas                     | Risco para passageiros                |
| Não há ranking de “melhor opção”                    | Decisão por sorte, não por critério   |

### Dados de entrada do problema

O sistema recebe, para cada usuário:

- Identidade (nome, e-mail, curso)
- **Bairro de origem**
- **Universidade de destino**
- Papel (motorista ou passageiro)
- Veículo (se motorista): modelo, placa, cor, **vagas disponíveis**

Para cada rota:

- Bairro de origem
- Destino (universidade)
- **Horário de saída**
- Vagas disponíveis
- Tipo (IDA / VOLTA)

Para uma busca:

- ID do usuário solicitante
- **Horário desejado**
- Destino opcional

### Saída esperada

Uma **lista ordenada de caronas compatíveis**, contendo:

- Motorista (nome, curso, avaliação)
- Bairro de origem da rota
- Horário de saída
- Vagas disponíveis
- **Score 0–100** (quanto maior, melhor o match)
- Diferença em minutos entre o horário pedido e o ofertado
- Explicação textual da compatibilidade

### Por que isso é um problema de “organização e processamento de dados”

Porque conforme a base de usuários cresce, a complexidade de encontrar
o match certo cresce de forma combinatória. Sem estrutura algorítmica
adequada:

- Procurar usuários por bairro vira O(n) (varre tudo).
- Combinar usuários compatíveis vira O(n²) (compara todos contra todos).
- Ordenar os resultados manualmente vira O(n²) com bubble sort.

Com as estruturas certas (Hash + Grafo + QuickSort), reduzimos para
**O(n log n)** no pior caso do pipeline completo — viável mesmo para
milhares de usuários.

---

## 2. Escolha dos Algoritmos

> O enunciado pede **no mínimo dois algoritmos**. Implementamos **quatro**,
> cada um resolvendo uma parte distinta do problema, e todos foram
> implementados **do zero** (não usamos `HashMap`, `Collections.sort`
> ou bibliotecas de grafo prontas).

### Algoritmo 1 — Tabela Hash (Encadeamento Separado)

**O que ele resolve neste sistema?**
Indexar usuários e rotas para que a busca por bairro
(“todos que saem de Nova Betânia”) seja praticamente instantânea.

**Como ele faz, em etapas:**
1. A chave (ex.: nome do bairro) passa por uma **função de hash**
   que devolve um número inteiro.
2. Esse número é convertido em um índice de array
   (`Math.floorMod(hash, capacidade)`).
3. Na posição calculada existe uma **lista encadeada de entradas**
   (encadeamento separado). Se houver colisão, o item é adicionado
   nessa lista; caso contrário, ele é o único.
4. Quando a tabela atinge **fator de carga > 0.75**, a capacidade
   **dobra** e todos os elementos são re-inseridos no novo array.

**Big-O:**

| Operação           | Caso médio | Pior caso |
|--------------------|-----------:|----------:|
| `put` (inserir)    | O(1)       | O(n)      |
| `get` (buscar)     | O(1)       | O(n)      |
| `remove`           | O(1)       | O(n)      |
| Re-hash (resize)   | O(n) amortizado | O(n)  |

**Por que foi escolhido?**
Porque a operação mais frequente do sistema é “me dê todos os usuários
do bairro X”. Sem hash, essa busca seria O(n) (varrer toda a lista).
Com hash, é O(1) médio — escala para milhares de usuários sem perda
de desempenho.

**Onde fica no código?**
`backend/src/main/java/com/ufersa/caronas/structures/TabelaHash.java`

---

### Algoritmo 2 — Grafo (Lista de Adjacência) + BFS

**O que ele resolve neste sistema?**
Representar **relações de compatibilidade** entre usuários:
quem mora perto, em bairros vizinhos, no mesmo horário,
indo para a mesma universidade. A partir do solicitante,
o grafo identifica todo o **cluster de pessoas alcançáveis**.

**Como ele faz, em etapas:**
1. Cada usuário vira um **nó** do grafo.
2. Para cada par de usuários, o sistema verifica se há compatibilidade
   (mesma universidade + bairros vizinhos + janela de horário ≤ 30 min).
3. Se houver, cria uma **aresta** entre os dois nós.
4. A representação interna é uma **lista de adjacência**
   (mais econômica em memória que matriz, para grafos esparsos).
5. A partir do solicitante, roda **BFS (Busca em Largura)** para
   encontrar todos os usuários do mesmo componente conectado —
   incluindo conexões transitivas (“vizinho do meu vizinho”).

**Big-O:**

| Operação           | Complexidade |
|--------------------|-------------:|
| `addNo`            | O(1)         |
| `addAresta`        | O(grau)      |
| `vizinhos(n)`      | O(grau(n))   |
| **BFS**            | O(V + E)     |

**Por que foi escolhido?**
Porque compatibilidade entre usuários é **uma relação**, não um
atributo isolado. Grafos são a estrutura natural para modelar
relações. Além disso, o **BFS** permite descobrir “clusters de
carona” — grupos de pessoas que naturalmente formariam uma rede.

**Onde fica no código?**
`backend/src/main/java/com/ufersa/caronas/structures/Grafo.java`

---

### Algoritmo 3 — QuickSort

**O que ele resolve neste sistema?**
Ordenar a lista final de matches por **score decrescente** (melhor
primeiro). Sem ordenação, o usuário receberia uma lista aleatória.

**Como ele faz, em etapas (divisão e conquista):**
1. Escolhe um **pivô** (usamos o elemento do meio para evitar pior
   caso em listas já ordenadas).
2. **Particiona** o array: elementos ≤ pivô vão para a esquerda,
   elementos > pivô vão para a direita.
3. Aplica o mesmo processo, **recursivamente**, nas duas metades.
4. Quando uma sub-lista tem 0 ou 1 elemento, ela já está ordenada.

**Big-O:**

| Cenário        | Complexidade |
|----------------|-------------:|
| Melhor caso    | O(n log n)   |
| **Caso médio** | **O(n log n)** |
| Pior caso      | O(n²) (raro com pivô do meio) |
| Memória        | O(log n) (pilha de recursão) |

**Por que foi escolhido?**
Comparamos com alternativas:

| Algoritmo      | Big-O médio | Comentário                                  |
|----------------|-------------|---------------------------------------------|
| Bubble Sort    | O(n²)       | Inviável para listas grandes                |
| Selection Sort | O(n²)       | Idem                                        |
| **QuickSort**  | **O(n log n)** | Excelente desempenho médio, in-place    |
| Merge Sort     | O(n log n)  | Estável, mas usa O(n) de memória extra      |

QuickSort entrega o melhor desempenho médio **sem custo extra de
memória significativo** — perfeito para o nosso caso.

**Onde fica no código?**
`backend/src/main/java/com/ufersa/caronas/structures/QuickSort.java`

---

### Algoritmo 4 — Dijkstra (caminho mínimo em grafo ponderado)

**O que ele resolve neste sistema?**
Depois que o usuário escolhe uma carona, calculamos o **melhor trajeto**
da corrida — motorista → passageiro → UFERSA — passando pelos bairros
mais próximos. É o que o frontend desenha no mapa.

**Como ele faz, em etapas:**
1. Inicializa a distância de todos os bairros como **infinito**, exceto
   a origem (0).
2. Coloca a origem em uma **fila de prioridade** (min-heap) ordenada
   pela menor distância.
3. Enquanto a fila não estiver vazia:
   - Pega o bairro com **menor distância acumulada**.
   - Para cada vizinho desse bairro, calcula a nova distância e atualiza
     se for melhor que a conhecida.
4. Quando chega ao destino, reconstrói o caminho mais curto via
   estrutura de "anteriores".

Operamos sobre um **GrafoPonderado** (lista de adjacência com pesos),
onde as arestas têm peso em **km calculados via fórmula de Haversine**
entre as coordenadas reais dos bairros de Mossoró/RN.

**Big-O:**

| Implementação                                | Complexidade        |
|----------------------------------------------|---------------------|
| **Com fila de prioridade (heap binário)**    | **O((V + E) log V)** |
| Sem heap (busca linear pelo próximo)         | O(V²)               |

Como a malha de bairros é pequena (~14 nós), o cálculo é instantâneo.

**Por que foi escolhido?**
Dijkstra é o algoritmo clássico para "menor caminho" em grafos com
pesos não-negativos. Comparado a BFS comum (que ignora pesos),
Dijkstra considera **distâncias reais** — então o trajeto sugerido
não é só "menos paradas", é **menos quilômetros**.

**Onde fica no código?**
`backend/src/main/java/com/ufersa/caronas/structures/Dijkstra.java`
`backend/src/main/java/com/ufersa/caronas/structures/GrafoPonderado.java`

---

### Como os quatro trabalham juntos (pipeline do match)

Quando o usuário clica em **Buscar carona**, acontece o seguinte:

```
┌─────────────────────────────────────────────────────────────┐
│ 1. HASH:                                                    │
│    rotasPorBairro.get("Nova Betânia") → 3 candidatos        │
│    rotasPorBairro.get("Centro")        → 4 candidatos       │
│    (bairros vizinhos do solicitante, lookup O(1) cada)      │
├─────────────────────────────────────────────────────────────┤
│ 2. GRAFO:                                                   │
│    Constrói grafo de compatibilidade com os candidatos.     │
│    BFS a partir do solicitante → cluster de N pessoas       │
│    realmente alcançáveis (filtra incompatíveis).            │
├─────────────────────────────────────────────────────────────┤
│ 3. FILTRO + SCORE:                                          │
│    Para cada candidato no cluster, aplica filtros de        │
│    universidade, horário (±30 min), vagas, motorista        │
│    válido e calcula um score 0–100.                         │
├─────────────────────────────────────────────────────────────┤
│ 4. QUICKSORT:                                               │
│    Ordena por score decrescente → resposta ao usuário.      │
├─────────────────────────────────────────────────────────────┤
│ 5. DIJKSTRA (quando o usuário clica em "Ver trajeto"):      │
│    Calcula o caminho mínimo                                 │
│    motorista → passageiro → UFERSA                          │
│    no grafo ponderado de bairros e desenha no mapa.         │
└─────────────────────────────────────────────────────────────┘
```

Complexidade total amortizada: **O(V + E + k log k + (V+E) log V)**
— a busca dos matches mais o cálculo do trajeto continuam viáveis
mesmo com milhares de usuários.

---

## 3. Produto

### O que foi entregue

Um **sistema web completo** com:

- **Backend Spring Boot** (Java) com API REST, expondo endpoints para
  CRUD de usuários, rotas e busca de matches.
- **Frontend HTML/CSS/JS** com interface visual moderna (estética
  editorial inspirada no sertão norte-riograndense).
- **Estruturas de dados implementadas do zero** (Hash, Grafo, QuickSort)
  no pacote `structures/`.
- **Dados de exemplo pré-carregados** para demonstração imediata.
- **Documentação completa** em `docs/`.

### Funcionalidades

- ✅ Cadastro de usuário (passageiro ou motorista, com dados do veículo)
- ✅ Cadastro de rota (origem, destino, horário, vagas, ida/volta)
- ✅ Busca de caronas compatíveis com **ranking por score**
- ✅ Avaliação de motoristas (1–5 estrelas)
- ✅ Estatísticas em tempo real das estruturas (total de usuários, baldes ocupados na hash)
- ✅ Suporte para múltiplos bairros de Mossoró com **mapa de proximidade** entre eles

### Tecnologias

- **Linguagem backend:** Java 17
- **Framework backend:** Spring Boot 3.3
- **Frontend:** HTML5, CSS3 (com fontes Instrument Serif + DM Sans), JavaScript ES6
- **Build:** Maven

### Como rodar
Ver [`README.md`](../README.md) na raiz do projeto.

---

## 4. Apresentação

> Este projeto tem um roteiro de apresentação dedicado em
> [`APRESENTACAO.md`](APRESENTACAO.md). Abaixo está a resposta direta
> às três perguntas do critério 4.

### 4.1 Qual era o problema antes?

Estudantes da UFERSA dependiam de **grupos de WhatsApp desorganizados**
para encontrar caronas. As mensagens se perdiam, não havia filtragem
por bairro ou horário, motoristas e passageiros não conseguiam
encontrar uns aos outros de forma eficiente. Não havia ranking,
avaliação ou critério objetivo para escolher uma carona — apenas
sorte.

**Requisitos do problema** (dados de entrada que precisam ser
organizados): nome, e-mail, curso, **bairro**, **universidade**,
papel (motorista/passageiro), veículo (se motorista), **rotas com
horário de saída** e vagas, e **uma janela de horário desejada para
a busca**.

### 4.2 Como o algoritmo resolve isso?

Três estruturas trabalham em pipeline:

1. **Tabela Hash** (custom, encadeamento separado) — indexa todos os
   usuários e rotas por bairro. Buscar “quem sai de Nova Betânia?”
   custa O(1) na média, contra O(n) numa lista comum.

2. **Grafo** (lista de adjacência, custom) — modela compatibilidade
   entre usuários como uma rede. Uma busca em largura (BFS) partindo
   do solicitante descobre o cluster de pessoas alcançáveis,
   incluindo conexões transitivas (vizinho do meu vizinho).

3. **QuickSort** (custom) — ordena os matches por score decrescente.
   Comparado a bubble sort (O(n²)), nosso algoritmo entrega
   **O(n log n)** no caso médio — várias ordens de magnitude mais
   rápido para listas grandes.

**Comparação com solução menos eficiente:**

| Operação                | Solução ingênua | Nossa solução |
|-------------------------|-----------------|---------------|
| Buscar por bairro       | O(n)            | **O(1)**      |
| Achar compatíveis       | O(n²) (par a par) | **O(V+E)** (BFS no grafo já construído) |
| Ordenar matches         | O(n²) bubble sort | **O(n log n)** QuickSort |

### 4.3 Qual o ganho/benefício?

- **Tempo:** busca de carona em milissegundos vs. minutos rolando
  WhatsApp.
- **Dinheiro:** divisão de combustível reduz custo de transporte do
  estudante.
- **Qualidade de vida:** menos estresse para chegar à universidade.
- **Ambiental:** menos carros nas ruas, menos poluição.
- **Social:** estudantes que andam juntos criam vínculo e aumentam
  a sensação de segurança coletiva.
- **Acadêmico:** demonstra na prática o impacto da escolha correta
  de estruturas de dados num problema real.

---

## Conclusão

O sistema atende aos quatro critérios da A3:
identifica um problema real e socialmente relevante, escolhe e
justifica **quatro algoritmos** (Hash, Grafo, QuickSort e Dijkstra —
duas vezes o mínimo exigido) com suas respectivas complexidades,
entrega um **produto funcional** acessível via interface web
(com mapa interativo OpenStreetMap mostrando o trajeto calculado),
e tem um material de apresentação que demonstra **claramente o ganho**
sobre a solução ingênua.
