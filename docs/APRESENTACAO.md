# Roteiro de Apresentação

> Duração sugerida: **10–12 minutos**.
> Atende ao **Critério 4** da A3.

---

## Slide / Momento 1 — Abertura (1 min)

> "Nosso projeto resolve um problema cotidiano universitário usando
> estruturas de dados clássicas para tornar a busca por caronas
> rápida, organizada e eficiente."

Apresente o time e diga em uma frase o que vão mostrar.

---

## Slide / Momento 2 — O problema (2 min)

**Pergunta 1 do critério 4: Qual era o problema antes?**

Mostre uma captura de tela de um grupo de WhatsApp típico de
caronas (bagunçado, com dezenas de mensagens fora de ordem). Aponte:

- Mensagens se perdem no scroll.
- Não há filtragem por bairro, horário ou universidade.
- Mesma pergunta ("alguém vai pra UFERSA às 7h?") repetida 10x.
- Sem avaliação dos motoristas — risco para passageiros.
- Decisão por sorte, não por critério.

> "Esse é um problema real — vivido todo dia por estudantes da
> UFERSA. Não é só um inconveniente: é tempo perdido, dinheiro
> gasto, e até segurança comprometida."

### Requisitos do problema (dados de entrada)

Liste no slide:

- Identidade do aluno (nome, e-mail, curso)
- **Bairro de origem**
- **Universidade**
- Papel (motorista / passageiro)
- Veículo (se motorista)
- Rotas com **horário de saída** e vagas
- Janela de horário desejada na busca

---

## Slide / Momento 3 — Demo ao vivo (3 min)

Abra o sistema (já com o backend rodando e o seed carregado):

1. Mostre a tela inicial (visual diferenciado, estética "Mossoró").
2. Vá ao **Cadastro** e crie um usuário rápido como passageiro:
   - Bairro: **Aeroporto**
3. Cadastre uma **rota** desse usuário com horário **07:15**.
4. Vá em **Buscar carona** e busque com horário **07:15**.
5. Mostre os resultados: 3+ matches ordenados por score, com
   bairros vizinhos, motoristas com avaliação diferente,
   compatibilidade explicada em texto.
6. **Aponte para os badges HASH / GRAFO / QUICKSORT** no resultado
   e diga: "Cada match passou por essas três etapas".

---

## Slide / Momento 4 — Como o algoritmo resolve (3 min)

**Pergunta 2 do critério 4: Como o algoritmo resolve isso?**

Use o diagrama do pipeline (de `ALGORITMOS.md`):

```
USUÁRIO clica Buscar
   │
   ▼
1. HASH — busca usuários por bairro em O(1)
   │
   ▼
2. GRAFO — BFS descobre cluster compatível em O(V + E)
   │
   ▼
3. FILTROS + SCORE — calcula 0–100 baseado em
   avaliação, proximidade, horário e vagas
   │
   ▼
4. QUICKSORT — ordena por score decrescente em O(n log n)
```

### Comparação com solução ingênua

| Operação                | Solução ingênua | Nossa solução |
|-------------------------|-----------------|---------------|
| Buscar usuários por bairro | O(n) (varre tudo) | **O(1)** (hash) |
| Achar todos compatíveis | O(n²) (par a par) | **O(V+E)** (grafo + BFS) |
| Ordenar matches         | O(n²) (bubble)    | **O(n log n)** (quicksort) |

**Mensagem-chave para o avaliador:**

> "Se a base crescesse para 10.000 estudantes, a solução ingênua
> levaria minutos. A nossa responde em **milissegundos** porque
> escolhemos as estruturas certas."

---

## Slide / Momento 5 — Detalhe de uma estrutura (1.5 min)

Escolha **uma** estrutura para detalhar (a Tabela Hash funciona bem
porque é mais visual). Mostre:

- A chave passa pela função hash.
- O resultado vira índice de array.
- Em caso de colisão, o item entra na lista encadeada daquela posição.
- Quando o array enche, redimensiona dobrando.

**Diga claramente:** "Não usamos `HashMap`. Implementamos do zero,
está em `backend/src/main/java/com/ufersa/caronas/structures/TabelaHash.java`."

Mostre o endpoint `/api/usuarios/stats` rodando ao vivo:

```json
{
  "totalUsuarios": 9,
  "totalBairros": 7,
  "baldesOcupadosNoIndiceBairro": 6
}
```

> "Com 7 chaves diferentes ocupando 6 baldes (em 16 disponíveis),
> nossa função hash distribui bem — quase sem colisões."

---

## Slide / Momento 6 — Benefícios (1.5 min)

**Pergunta 3 do critério 4: Qual o ganho?**

Use ícones para cada ponto:

| 💰 | **Econômico** | Divisão de combustível reduz custo do estudante |
| ⏱️ | **Tempo** | Busca em ms vs. minutos no WhatsApp |
| 🌱 | **Ambiental** | Menos carros nas ruas, menos poluição |
| 🤝 | **Social** | Estudantes se conhecem, sensação de segurança |
| 🎓 | **Acadêmico** | Demonstração viva do impacto das estruturas de dados |

---

## Slide / Momento 7 — Encerramento (30 s)

> "Esse projeto mostra que estruturas de dados não são teoria isolada —
> são a diferença entre um sistema lento e inviável e um sistema que
> resolve um problema real de forma elegante. Tabela Hash, Grafo e
> QuickSort, implementados do zero, são a base do nosso sistema de
> caronas universitárias."

Abra para perguntas.

---

## Anexo — perguntas que o professor pode fazer

**P: Por que não usaram o `HashMap` do Java?**
R: Porque o objetivo da A3 é demonstrar implementação e domínio
das estruturas, não o uso. Usar `HashMap` seria como colar a
resposta. Nossa `TabelaHash` cumpre o mesmo papel, com encadeamento
separado e redimensionamento automático.

**P: O que acontece se duas chaves colidirem?**
R: Encadeamento separado: ambas vão para a mesma posição do array,
ligadas numa lista encadeada. A busca passa por essa lista — em
geral, com 1 ou 2 elementos.

**P: Qual a vantagem de Grafo sobre uma simples lista filtrada?**
R: Uma lista filtrada testaria todos os usuários contra o solicitante
em O(n). O grafo permite encontrar **conexões transitivas** ("vizinho
do meu vizinho") em O(V + E) com BFS, e ainda pré-computa os
relacionamentos.

**P: Quando o QuickSort vai ao pior caso O(n²)?**
R: Quando o pivô escolhido é sempre o menor ou o maior elemento.
Por isso escolhemos o **elemento do meio** como pivô — torna o pior
caso extremamente raro em dados reais (caso patológico exigiria
listas construídas propositalmente).

**P: O sistema escala?**
R: Sim. Como a busca por bairro é O(1) e o ranking é O(n log n) sobre
os matches (não sobre o total de usuários), o sistema responde bem
mesmo com dezenas de milhares de cadastrados. O gargalo seria, em
escala muito maior, a construção do grafo — mas isso pode ser
otimizado com pre-computação incremental.

**P: Como tratam horários no fim do dia (23:50 vs. 00:10)?**
R: Atualmente a diferença é calculada em minutos absolutos no dia,
então 23:50 e 00:10 dariam 1420 min de diferença. Para o caso
universitário (entradas 6h, 7h, 13h, 18h), isso não é um problema
real, mas seria fácil estender com aritmética circular se necessário.
