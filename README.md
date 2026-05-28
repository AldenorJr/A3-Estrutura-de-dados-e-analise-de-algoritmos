# Caronas UFERSA — Sistema de Match para Caronas Universitárias

> **Projeto A3 — Estruturas de Dados e Análise de Algoritmos**
>
> Sistema que combina **Tabela Hash**, **Grafo** e **QuickSort** (todos
> implementados do zero, sem usar `HashMap` ou `Collections.sort`) para
> conectar estudantes que vão para a mesma universidade em horários
> compatíveis.

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

### 1. Pré-requisitos

- **Java 17 ou superior** (`java -version`)
- **Maven 3.8+** (`mvn -version`)
- Um navegador moderno (Chrome, Firefox, Safari, Edge)

### 2. Subir o backend

```bash
cd backend
mvn spring-boot:run
```

Aguarde aparecer no log:

```
Seed concluido: 9 usuarios e 8 rotas cadastradas.
Acesse http://localhost:8080/api/usuarios
```

A API fica em `http://localhost:8080`.

### 3. Abrir o frontend

Basta abrir o arquivo `frontend/index.html` no navegador
(duplo clique ou arrastar para a janela).

Se preferir servir via HTTP local (recomendado para evitar avisos
de CORS em alguns navegadores):

```bash
cd frontend
python3 -m http.server 5500
# acesse http://localhost:5500
```

> O backend já está com CORS aberto para qualquer origem
> (`/api/**`), então a página pode rodar diretamente em `file://`
> em Chrome e Firefox.

### 4. Usar

1. Preencha o **Cadastro** (marque “Sou motorista” para oferecer caronas).
2. Cadastre uma **Rota** com bairro de origem e horário.
3. Vá para **Buscar carona** e informe um horário desejado.
4. Veja os matches ordenados por score.

Já existem **9 usuários e 8 rotas de exemplo** pré-carregados
(motoristas e passageiros em diferentes bairros de Mossoró), então
você pode buscar caronas imediatamente após subir o sistema.

---

## Estrutura do projeto

```
PROJETO A3 ALGORITMO/
├── backend/                                # Spring Boot
│   ├── pom.xml
│   └── src/main/java/com/ufersa/caronas/
│       ├── CaronasApplication.java
│       ├── config/CorsConfig.java
│       ├── model/         (Usuario, Rota, Veiculo, MatchResult, TipoRota)
│       ├── structures/    ★ ALGORITMOS DO PROJETO
│       │   ├── TabelaHash.java     # hash custom (encadeamento separado)
│       │   ├── Grafo.java          # grafo + BFS
│       │   └── QuickSort.java      # quicksort genérico
│       ├── service/       (UsuarioService, RotaService, MatchService, BairroService)
│       ├── controller/    (REST endpoints)
│       ├── dto/           (objetos de transferência)
│       └── seed/SeedDataRunner.java  # dados de exemplo
│
├── frontend/                               # HTML/CSS/JS puro
│   ├── index.html
│   ├── css/style.css
│   └── js/
│       ├── api.js      # cliente da API REST
│       └── app.js      # lógica de UI
│
└── docs/                                   # Documentação acadêmica
    ├── DOCUMENTACAO.md   # documento principal (atende 4 critérios A3)
    ├── ALGORITMOS.md     # detalhamento técnico Hash/Grafo/QuickSort
    ├── ARQUITETURA.md    # diagramas e fluxo
    └── APRESENTACAO.md   # roteiro de apresentação
```

---

## Endpoints REST (resumo)

| Método | Rota                                  | O que faz                              |
|--------|---------------------------------------|----------------------------------------|
| GET    | `/api/bairros`                        | Lista bairros suportados               |
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
Universidade: **UFERSA** — Universidade Federal Rural do Semi-Árido
