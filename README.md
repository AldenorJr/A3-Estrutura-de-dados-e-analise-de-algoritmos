# Carona Universidades — Sistema de Match para Caronas Universitárias

> **Projeto A3 — Estruturas de Dados e Análise de Algoritmos**
>
> Sistema que combina **Tabela Hash**, **Grafo**, **QuickSort** e
> **Dijkstra** (todos implementados do zero, sem usar `HashMap`,
> `Collections.sort` ou bibliotecas de grafo prontas) para conectar
> estudantes da **UFERSA**, **UERN** e **IFRN** que vão para a mesma
> universidade em horários compatíveis, e calcular o melhor trajeto
> da corrida com mapa interativo.

---

## Visão rápida

| Camada       | Tecnologia                     | Pasta      |
|--------------|--------------------------------|------------|
| Backend      | Java 17 + Spring Boot 3.3      | `backend/` |
| Frontend     | HTML + CSS + JavaScript puro   | `frontend/`|
| Documentação | Markdown                       | `docs/`    |

Não há banco de dados — todo o armazenamento é em memória usando a
**Tabela Hash custom** do projeto. Isso simplifica a execução e
demonstra o uso real da estrutura de dados.

---

## Como rodar

Tem dois caminhos: **Docker (1 comando, recomendado)** ou
**execução local com Maven**.

### Opção A — Docker Compose (recomendado)

Único pré-requisito: ter o **Docker Desktop** (ou Docker Engine +
Compose v2) instalado.

```bash
docker compose up -d
```

A primeira vez baixa imagens e compila o backend (~1–2 min).
Depois sobe em ~30 s. Acessos:

- **Frontend:** http://localhost:5500
- **Backend:**  http://localhost:8080

Para acompanhar logs:

```bash
docker compose logs -f backend
```

Para derrubar:

```bash
docker compose down
```

---

### Opção B — Local (sem Docker)

Pré-requisitos:

- **Java 17 ou superior** (`java -version`)
- **Maven 3.8+** (`mvn -version`)
- Um navegador moderno (Chrome, Firefox, Safari, Edge)
- **Python 3** (apenas para servir o frontend; opcional)

**1. Subir o backend:**

```bash
cd backend
mvn spring-boot:run
```

Aguarde aparecer no log:

```
Seed concluido: 19 usuarios e 28 rotas cadastradas.
Universidades: UFERSA, UERN, IFRN
Acesse http://localhost:8080/api/usuarios
```

A API fica em `http://localhost:8080`.

**2. Abrir o frontend:**

Basta abrir o arquivo `frontend/index.html` no navegador
(duplo clique ou arrastar para a janela).

Se preferir servir via HTTP local:

```bash
cd frontend
python3 -m http.server 5500
# acesse http://localhost:5500
```

> O backend já está com CORS aberto para qualquer origem
> (`/api/**`), então a página pode rodar diretamente em `file://`
> em Chrome e Firefox.

---

### Usar a aplicação

1. Preencha o **Cadastro** (marque “Sou motorista” para oferecer caronas).
2. Cadastre uma **Rota** com bairro de origem, **universidade** e horário.
3. Vá para **Buscar carona**, escolha **universidade** e horário desejado.
4. Veja os matches ordenados por score.
5. Clique em **"Ver trajeto no mapa →"** num match: abre modal com
   mapa Leaflet, sequência de paradas (motorista → você → universidade),
   distância em km, tempo estimado e economia em R$.

Já existem **19 usuários e 28 rotas de exemplo** pré-carregados
(motoristas e passageiros em diferentes bairros de Mossoró,
distribuídos entre UFERSA, UERN e IFRN), então você pode buscar
caronas imediatamente após subir o sistema.

---

## Estrutura do projeto

```
PROJETO A3 ALGORITMO/
├── docker-compose.yml                       # sobe tudo: docker compose up -d
├── backend/                                 # Spring Boot
│   ├── Dockerfile                           # imagem multi-stage (build + runtime)
│   ├── pom.xml
│   └── src/main/java/com/ufersa/caronas/
│       ├── CaronasApplication.java
│       ├── config/CorsConfig.java
│       ├── model/         (Usuario, Rota, Veiculo, Coordenada, MatchResult, ...)
│       ├── structures/    ★ ALGORITMOS DO PROJETO
│       │   ├── TabelaHash.java        # hash custom (encadeamento separado)
│       │   ├── Grafo.java             # grafo de compatibilidade + BFS
│       │   ├── GrafoPonderado.java    # grafo com pesos (bairros)
│       │   ├── Dijkstra.java          # caminho mínimo
│       │   └── QuickSort.java         # quicksort genérico
│       ├── service/       (UsuarioService, RotaService, MatchService,
│       │                   BairroService, TrajetoriaService)
│       ├── controller/    (REST endpoints)
│       ├── dto/           (objetos de transferência)
│       └── seed/SeedDataRunner.java   # 19 usuários + 28 rotas
│
├── frontend/                                # HTML/CSS/JS puro
│   ├── Dockerfile                           # imagem nginx alpine
│   ├── nginx.conf                           # gzip + cache
│   ├── index.html
│   ├── css/style.css
│   └── js/
│       ├── api.js      # cliente da API REST
│       └── app.js      # lógica de UI + mapa Leaflet + OSRM
│
└── docs/                                    # Documentação acadêmica
    ├── DOCUMENTACAO.md   # documento principal (atende 4 critérios A3)
    ├── ALGORITMOS.md     # detalhamento técnico Hash/Grafo/QuickSort/Dijkstra
    ├── ARQUITETURA.md    # diagramas e fluxo
    └── APRESENTACAO.md   # roteiro de apresentação
```

---

## Endpoints REST (resumo)

| Método | Rota                                  | O que faz                              |
|--------|---------------------------------------|----------------------------------------|
| GET    | `/api/bairros`                        | Lista bairros suportados               |
| GET    | `/api/universidades`                  | Lista universidades suportadas         |
| GET    | `/api/usuarios`                       | Lista todos os usuários                |
| POST   | `/api/usuarios`                       | Cria usuário                           |
| GET    | `/api/usuarios/{id}`                  | Busca por id                           |
| GET    | `/api/usuarios/bairro/{bairro}`       | Busca por bairro (usa Hash O(1))       |
| POST   | `/api/usuarios/avaliar`               | Avalia um motorista (0–5)              |
| GET    | `/api/usuarios/stats`                 | Estatísticas da hash                   |
| GET    | `/api/rotas`                          | Lista rotas                            |
| POST   | `/api/rotas`                          | Cria rota                              |
| GET    | `/api/rotas/usuario/{id}`             | Rotas de um usuário                    |
| GET    | `/api/match?usuarioId=&horario=HH:mm` | **Busca caronas compatíveis ordenadas**|
| GET    | `/api/trajetoria?passageiroId=&rotaId=` | **Calcula melhor trajeto (Dijkstra)** |
| GET    | `/api/trajetoria/coordenadas`         | Coordenadas dos bairros (para o mapa)  |

Exemplo de chamada manual:

```bash
curl "http://localhost:8080/api/match?usuarioId=6&horario=07:15"
```

---

## Documentação completa

- [`docs/DOCUMENTACAO.md`](docs/DOCUMENTACAO.md) — **documento principal**: cobre os 4 critérios da A3.
- [`docs/ALGORITMOS.md`](docs/ALGORITMOS.md) — explicação detalhada de cada algoritmo, com Big-O e exemplos.
- [`docs/ARQUITETURA.md`](docs/ARQUITETURA.md) — diagramas e fluxo de dados.
- [`docs/APRESENTACAO.md`](docs/APRESENTACAO.md) — roteiro pronto para apresentar.

---

## Créditos

Disciplina: **Estruturas de Dados e Análise de Algoritmos**
Avaliação: **A3**
Universidades suportadas no demo: **UFERSA, UERN, IFRN** (Mossoró/RN)
