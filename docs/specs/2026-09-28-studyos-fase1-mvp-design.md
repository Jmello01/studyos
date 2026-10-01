# StudyOS — Fase 1 (MVP) — Design

- **Data:** 2026-09-28
- **Status:** aguardando revisão
- **Escopo:** Fase 1 — Dashboard, Áreas, Matérias, Tópicos, Planejamento, Sessões, métricas básicas

---

## 1. Contexto e objetivos

StudyOS é um PWA pessoal para **planejar, registrar e analisar estudos**, seguindo o ciclo:

```
PLANEJAR → ESTUDAR → REGISTRAR → MEDIR → ANALISAR → AJUSTAR
```

Dois objetivos simultâneos:

1. **Ferramenta real**, usada diariamente, substituindo gradualmente a planilha atual.
2. **Projeto de aprendizado** em Java + Spring Boot + Angular + PostgreSQL + Docker, construído de forma guiada.

### Perfil de quem desenvolve (define o ritmo)

| Tecnologia | Nível atual |
|---|---|
| Java | básico |
| Spring Boot | nunca usou |
| Angular / TypeScript | nunca usou |
| Git | básico (commit e push) |
| IDE | IntelliJ |

### Critério de sucesso do MVP

O usuário consegue, sem a planilha:

1. Cadastrar áreas, matérias e tópicos.
2. Montar uma semana-modelo e ajustar a semana atual.
3. Registrar cada sessão de estudo em menos de 15 segundos.
4. Ver no dashboard, para qualquer período: horas realizadas × planejadas, % de cumprimento, dias estudados, sequência, foco médio, horas por dia, por matéria e por área.
5. Comparar o período atual com o anterior.

---

## 2. Decisões tomadas no brainstorming

| # | Decisão | Motivo |
|---|---|---|
| D1 | Projeto em `C:\Projetos\studyos`, fora do OneDrive | Sincronização de `node_modules/` e `target/` causa travamentos; o backup do código é Git + GitHub |
| D2 | Planejamento = **semana-modelo + semana concreta ajustável** | A rotina se repete, mas cada semana tem imprevistos; o histórico do que foi planejado fica preservado |
| D3 | Sessão **não tem vínculo** com item do plano; cumprimento calculado **por agregação** | Registro rápido; estudo fora do plano conta; métricas via `SUM … GROUP BY` |
| D4 | Hierarquia **Área → Matéria → Tópico**; "atividade" é um `enum` na sessão | O 4º nível do exemplo era sub-tópico, não atividade; evita uma tabela sem uso real (YAGNI) |
| D5 | Construção em **fatias verticais**; em cada fatia o backend vem primeiro e é testado isoladamente | Sistema utilizável desde a Fatia 1, sem aprender duas tecnologias novas ao mesmo tempo |
| D6 | Item de plano sempre aponta para uma **matéria** (ex.: "Faculdade → Algoritmos") | Métricas planejado × realizado sempre no mesmo nível |
| D7 | Sem Lombok nem MapStruct; DTOs como `record` | Nada de código gerado invisível enquanto se aprende |

---

## 3. Escopo

### Dentro da Fase 1

- CRUD + arquivamento de Áreas, Matérias e Tópicos; progresso de tópico.
- Semana-modelo e semanas concretas.
- Registro, edição e exclusão de sessões.
- Dashboard com seletor de período (Hoje, Semana, Mês, Trimestre, Ano, intervalo personalizado) e comparação com o período anterior.
- Tema claro/escuro; layout responsivo (sidebar no desktop, bottom nav no mobile).
- PWA básico (instalável: manifest, ícones, service worker padrão do Angular).
- Seed das 9 áreas iniciais.

### Fora da Fase 1 (fases futuras)

Timer/Pomodoro, status de sessão, metas globais e por área, metas por período, gamificação, Faculdade (provas, notas, frequência), Projetos, Livros, LeetCode, vocabulário de Inglês, autenticação/usuários, offline com sincronização, deploy em cloud, CI/CD, importação da planilha.

---

## 4. Arquitetura

### Repositório (monorepo)

```
studyos/
├── backend/              Spring Boot (Maven, Java 21)
├── frontend/             Angular (Angular CLI)
├── docker-compose.yml    PostgreSQL 17
├── .env.example          variáveis do banco (o .env real não vai para o Git)
├── docs/                 specs, planos, anotações de aprendizado
├── .gitignore
└── README.md
```

### Fluxo

```
Angular (4200) ──/api (proxy)──▶ Spring Boot (8080) ──JDBC──▶ PostgreSQL (Docker, host 5433 → container 5432)
                                 Controller → Service → Repository
```

- **Dev:** PostgreSQL via `docker compose up -d`; backend pelo IntelliJ (▶/debug); frontend via `ng serve` com `proxy.conf.json` (`/api` → `localhost:8080`), sem CORS.
- **Versões:** Java 21; Spring Boot 4.1.x; Angular 22; PostgreSQL 17.
- **Porta do banco:** 5433 no host, porque a máquina já tem um PostgreSQL local instalado na 5432.
- **Schema:** criado exclusivamente pelo **Flyway** (`spring.jpa.hibernate.ddl-auto=validate`).
- **Fuso horário:** a aplicação considera `America/Sao_Paulo` como fuso do usuário; `timestamptz` gravado em UTC (`hibernate.jdbc.time_zone=UTC`). `study_date` é uma data local enviada pelo cliente.

---

## 5. Banco de dados

Convenções: `snake_case`; PK `bigint generated always as identity`; `created_at`/`updated_at timestamptz not null default now()`; FKs com `ON DELETE RESTRICT`; índice em toda FK; enums gravados como `varchar`.

### 5.1 `areas`

| Coluna | Tipo | Regras |
|---|---|---|
| id | bigint PK | |
| name | varchar(100) | not null, **unique** |
| description | text | null |
| color | varchar(7) | null, formato `#RRGGBB` |
| icon | varchar(50) | null |
| position | int | not null default 0 (ordem de exibição) |
| archived | boolean | not null default false |
| created_at, updated_at | timestamptz | |

### 5.2 `subjects` (matérias)

| Coluna | Tipo | Regras |
|---|---|---|
| id | bigint PK | |
| area_id | bigint FK → areas | not null |
| name | varchar(100) | not null; **unique (area_id, name)** |
| description | text | null |
| color, icon | varchar | null |
| weekly_goal_minutes | int | null, `>= 0` |
| monthly_goal_minutes | int | null, `>= 0` |
| priority | varchar(10) | not null default `MEDIUM` — `LOW`, `MEDIUM`, `HIGH` |
| status | varchar(20) | not null default `NOT_STARTED` — `NOT_STARTED`, `IN_PROGRESS`, `PAUSED`, `COMPLETED` |
| archived | boolean | not null default false |
| created_at, updated_at | timestamptz | |

### 5.3 `topics` (tópicos)

| Coluna | Tipo | Regras |
|---|---|---|
| id | bigint PK | |
| subject_id | bigint FK → subjects | not null |
| name | varchar(100) | not null; **unique (subject_id, name)** |
| progress | smallint | not null default 0, `CHECK 0–100` |
| position | int | not null default 0 |
| archived | boolean | not null default false |
| created_at, updated_at | timestamptz | |

### 5.4 `study_sessions` (sessões)

| Coluna | Tipo | Regras |
|---|---|---|
| id | bigint PK | |
| study_date | date | not null |
| started_at | timestamptz | null |
| ended_at | timestamptz | null; se ambos preenchidos, `ended_at > started_at` |
| duration_minutes | int | not null, `CHECK 1–1440` |
| subject_id | bigint FK → subjects | not null |
| topic_id | bigint FK → topics | null |
| activity_type | varchar(20) | not null default `OTHER` — `READING`, `VIDEO_LESSON`, `EXERCISES`, `PROJECT`, `REVIEW`, `OTHER` |
| focus | smallint | null, `CHECK 1–5` |
| notes | text | null |
| created_at, updated_at | timestamptz | |

Índices: `(study_date)`, `(subject_id, study_date)`, `(topic_id)`.

> Os valores dos enums ficam em inglês no código e no banco (convenção de mercado); os rótulos em português ficam só no frontend (ex.: `VIDEO_LESSON` → "Vídeo-aula").

### 5.5 `plan_template_items` (semana-modelo)

| Coluna | Tipo | Regras |
|---|---|---|
| id | bigint PK | |
| day_of_week | smallint | not null, `CHECK 1–7` (1 = segunda, ISO-8601) |
| start_time | time | null |
| subject_id | bigint FK → subjects | not null |
| topic_id | bigint FK → topics | null |
| planned_minutes | int | not null, `CHECK 1–1440` |
| priority | varchar(10) | not null default `MEDIUM` |
| description | text | null |
| position | int | not null default 0 |
| created_at, updated_at | timestamptz | |

### 5.6 `weekly_plans` (semana concreta)

| Coluna | Tipo | Regras |
|---|---|---|
| id | bigint PK | |
| week_start | date | not null, **unique**; sempre uma segunda-feira (`CHECK extract(isodow from week_start) = 1`) |
| created_at | timestamptz | |

### 5.7 `weekly_plan_items`

| Coluna | Tipo | Regras |
|---|---|---|
| id | bigint PK | |
| weekly_plan_id | bigint FK → weekly_plans | not null, `ON DELETE CASCADE` (item não existe sem a semana) |
| plan_date | date | not null; deve estar entre `week_start` e `week_start + 6` |
| start_time | time | null |
| subject_id | bigint FK → subjects | not null |
| topic_id | bigint FK → topics | null |
| planned_minutes | int | not null, `CHECK 1–1440` |
| priority | varchar(10) | not null default `MEDIUM` |
| description | text | null |
| created_at, updated_at | timestamptz | |

Índices: `(weekly_plan_id)`, `(plan_date)`, `(subject_id, plan_date)`.

### 5.8 Migrations previstas

```
V1__create_areas.sql
V2__seed_areas.sql            FACULDADE, FULL STACK, INGLÊS, ALGORITMOS, BANCO DE DADOS,
                              CLOUD, PROJETOS, CARREIRA, LEITURA
V3__create_subjects.sql
V4__create_topics.sql
V5__create_study_sessions.sql
V6__create_plans.sql          plan_template_items, weekly_plans, weekly_plan_items
```

Cada migration nasce na fatia que a usa. Migration aplicada **nunca é editada**; correções viram uma nova migration.

---

## 6. Regras de negócio

### Catálogo

- **R1** Nome de área é único; nome de matéria é único dentro da área; nome de tópico é único dentro da matéria → `409 Conflict`.
- **R2** Nada com histórico é excluído: áreas, matérias e tópicos são **arquivados**. Não existe `DELETE` para eles no MVP.
- **R3** Matéria ou tópico arquivado não pode receber **nova** sessão ou item de plano → `422`. Sessões antigas continuam contando nas métricas.
- **R4** Arquivar uma área esconde suas matérias dos seletores, mas não altera as matérias.
- **R5** Progresso da matéria = média de `progress` dos tópicos **não arquivados**, arredondada; `null` se não houver tópicos. Calculado na consulta, nunca armazenado.

### Sessões

- **R6** Se `startedAt` e `endedAt` forem enviados, o backend calcula `durationMinutes` (arredondado para baixo) e ignora o valor enviado. Caso contrário, `durationMinutes` é obrigatório.
- **R7** Uma sessão que atravessa a meia-noite pertence ao `studyDate` informado (por padrão, o dia de início).
- **R8** `topicId`, se informado, deve pertencer ao `subjectId` → `422`.
- **R9** Sessão pode ser excluída (`DELETE`), porque um lançamento errado deve poder sumir.

### Planejamento

- **R10** `ensureWeeklyPlan(weekStart)`: se a semana não existe, cria `weekly_plans` e copia cada `plan_template_item` para `weekly_plan_items` com `plan_date = week_start + (day_of_week - 1)`. Operação **idempotente**: se a semana existe, não faz nada.
- **R11** `ensureWeeklyPlan` é chamado: (a) ao abrir uma semana no planejamento; (b) ao criar ou editar sessão (para a semana do `studyDate`); (c) pelo dashboard, para a semana atual quando ela estiver no intervalo consultado.
- **R12** Alterar a semana-modelo **não** altera semanas já geradas.
- **R13** `weekStart` que não seja segunda-feira → `400`.
- **R14** Limitação conhecida: uma semana passada que nunca foi aberta e não teve sessões não tem plano, então conta como 0 min planejado.

---

## 7. Métricas (dashboard)

Todas recebem um intervalo fechado `[from, to]` de datas.

| Métrica | Definição |
|---|---|
| Realizado | `SUM(duration_minutes)` das sessões com `study_date` no intervalo |
| Planejado | `SUM(planned_minutes)` dos `weekly_plan_items` com `plan_date` no intervalo |
| Cumprimento | `realizado / planejado × 100`, uma casa decimal; `null` (exibido "—") se planejado = 0. Pode passar de 100% |
| Sessões | `COUNT(*)` das sessões |
| Dias estudados | `COUNT(DISTINCT study_date)` |
| Foco médio | `AVG(focus)` das sessões com foco preenchido; `null` se nenhuma |
| Por matéria | realizado e planejado agrupados por matéria (inclui matérias com só um dos dois) |
| Por área | realizado e planejado agrupados pela área da matéria |
| Por dia | realizado e planejado para **cada** data do intervalo (datas sem dados vêm com 0) |
| Sequência atual | dias consecutivos com ≥ 1 sessão terminando **hoje**; se hoje ainda não tem sessão, terminando **ontem** (a sequência só quebra depois que o dia acaba) |
| Recorde | maior sequência de todo o histórico |

**Períodos** (calculados no frontend): semana = segunda a domingo; mês e ano = calendário; trimestre = jan–mar, abr–jun, jul–set, out–dez. **Período anterior** = mesmo tipo, imediatamente antes. Para intervalo personalizado, o anterior tem o mesmo número de dias, logo antes do `from`.

**Comparação neutra:** a variação é exibida em texto neutro ("sem. anterior 14h20 · +30,2%"), sem cores de bom/ruim.

---

## 8. API REST

Base `/api`, JSON em `camelCase`, datas ISO-8601 (`2026-09-28`, `14:30`, `2026-09-28T14:30:00-03:00`).

### Catálogo

```
GET    /api/areas?includeArchived=false
POST   /api/areas
GET    /api/areas/{id}
PUT    /api/areas/{id}
PATCH  /api/areas/{id}/archive
PATCH  /api/areas/{id}/unarchive

GET    /api/subjects?areaId=&status=&includeArchived=false
POST   /api/subjects
GET    /api/subjects/{id}                 inclui progress calculado (R5)
PUT    /api/subjects/{id}
PATCH  /api/subjects/{id}/archive | /unarchive

GET    /api/subjects/{id}/topics?includeArchived=false
POST   /api/topics
PUT    /api/topics/{id}
PATCH  /api/topics/{id}/progress          { "progress": 75 }
PATCH  /api/topics/{id}/archive | /unarchive
```

### Sessões

```
GET    /api/sessions?from=&to=&subjectId=&areaId=     ordenado por study_date desc, started_at desc
POST   /api/sessions
GET    /api/sessions/{id}
PUT    /api/sessions/{id}
DELETE /api/sessions/{id}
```

Exemplo de `POST /api/sessions`:

```json
{
  "studyDate": "2026-09-28",
  "startedAt": null,
  "endedAt": null,
  "durationMinutes": 82,
  "subjectId": 3,
  "topicId": 12,
  "activityType": "EXERCISES",
  "focus": 5,
  "notes": "Interfaces e classes abstratas"
}
```

### Planejamento

```
GET    /api/plan-template/items
POST   /api/plan-template/items
PUT    /api/plan-template/items/{id}
DELETE /api/plan-template/items/{id}

GET    /api/weekly-plans/{weekStart}                  gera a partir do modelo se não existir (R10)
POST   /api/weekly-plans/{weekStart}/items
PUT    /api/weekly-plans/{weekStart}/items/{id}
DELETE /api/weekly-plans/{weekStart}/items/{id}
```

### Dashboard

```
GET /api/dashboard/summary?from=&to=       { realizedMinutes, plannedMinutes, completionPct, sessions, studyDays, avgFocus }
GET /api/dashboard/by-subject?from=&to=    [{ subjectId, subjectName, areaName, realizedMinutes, plannedMinutes }]
GET /api/dashboard/by-area?from=&to=       [{ areaId, areaName, realizedMinutes, plannedMinutes }]
GET /api/dashboard/daily?from=&to=         [{ date, realizedMinutes, plannedMinutes }]
GET /api/dashboard/streak                  { current, longest }
```

`from > to` ou intervalo maior que 366 dias → `400`.

### Erros (`ProblemDetail`, RFC 9457)

| Status | Quando |
|---|---|
| 400 | Validação de DTO (`@Valid`), parâmetro inválido, `weekStart` que não é segunda |
| 404 | Recurso inexistente |
| 409 | Nome duplicado (R1) |
| 422 | Regra de negócio violada (R3, R8) |

```json
{
  "type": "about:blank",
  "title": "Validation failed",
  "status": 400,
  "detail": "Um ou mais campos são inválidos",
  "errors": { "durationMinutes": "deve ser maior que 0" }
}
```

---

## 9. Backend

Pacote base `com.studyos`, organizado **por camada**:

```
com.studyos
├── controller/    HTTP apenas: recebe DTO, valida, chama service, devolve status
├── service/       regras de negócio, @Transactional
├── repository/    Spring Data JPA; agregações com @Query + projeções em record
├── entity/        entidades JPA  (+ entity/enums: Priority, SubjectStatus, ActivityType)
├── dto/           records de request/response — entidade nunca vira JSON
├── mapper/        conversão entity ↔ DTO, escrita à mão
├── exception/     ResourceNotFoundException, BusinessException, ConflictException,
│                  GlobalExceptionHandler (@RestControllerAdvice → ProblemDetail)
└── config/        fuso horário, Jackson
```

**Dependências (start.spring.io):** Spring Web, Spring Data JPA, Validation, Flyway Migration, PostgreSQL Driver, Testcontainers, Spring Boot DevTools, Spring Boot Actuator (apenas `/actuator/health`, para verificar app + banco).

**Regras de camada:** controller não tem regra de negócio nem acessa repository; service não conhece HTTP; entidade não sai da API.

**Relacionamentos JPA:** `@ManyToOne(fetch = LAZY)` do lado filho; sem `@OneToMany` bidirecional no MVP, porque as listas são buscadas pelo repository do filho (evita N+1 e loops de serialização).

**Streak:** o repository devolve as datas distintas com sessão; o `DashboardService` calcula `current` e `longest` em Java.

### Testes

| Camada | Ferramenta | Exemplos |
|---|---|---|
| Service | JUnit 5 + Mockito | R6 cálculo de duração; R10 idempotência; streak com lacuna, com hoje vazio, sem sessões |
| Controller | `@WebMvcTest` + MockMvc | 400 com `errors`, 404, 409, status de criação `201` + `Location` |
| Repository / dashboard | `@DataJpaTest` + Testcontainers (PostgreSQL real) | agregações por matéria, área e dia; datas sem dados |

Testes manuais da API são feitos com o **Bruno** (cliente HTTP gratuito, coleção em arquivos de texto versionada em `backend/api/`), porque o IntelliJ Community não inclui o HTTP Client. Requisições `GET` simples também podem ser feitas no navegador ou com `Invoke-RestMethod`.

---

## 10. Frontend

```
frontend/src/app/
├── app.config.ts, app.routes.ts          rotas com lazy loading
├── core/
│   ├── api/        AreaApi, SubjectApi, TopicApi, SessionApi, PlanApi, DashboardApi
│   ├── models/     interfaces espelhando os DTOs
│   └── layout/     Shell (sidebar / bottom nav), ThemeService
├── shared/
│   ├── components/ card, empty-state, confirm-dialog, period-picker
│   └── pipes/      minutesToHours (90 → "1h30"), activityLabel
└── features/
    ├── dashboard/
    ├── sessions/   lista + formulário
    ├── planning/   semana concreta + semana-modelo
    └── catalog/    áreas → matérias → tópicos
```

- **Standalone components**; **Signals** para estado de tela; **RxJS/HttpClient** para chamadas HTTP; **Reactive Forms** com validação espelhando o backend.
- Componentes nunca usam `HttpClient` diretamente, só por `core/api`.
- **Estilo:** SCSS puro com design tokens (variáveis CSS), tema claro/escuro (preferência do sistema + escolha manual salva em `localStorage`). Sem biblioteca de componentes. Estética minimalista (Linear/Vercel).
- **Gráficos:** Chart.js via `ng2-charts`.
- **PWA:** `ng add @angular/pwa` ao final do MVP.

### Rotas e telas

| Rota | Tela | Pontos-chave |
|---|---|---|
| `/dashboard` | Dashboard | Seletor de período + ← →; 4 indicadores (realizado/planejado, cumprimento, dias estudados/sequência, foco médio); gráfico diário planejado × realizado; lista por matéria; comparação neutra com o período anterior; botão "+ Registrar" |
| `/sessions` | Histórico | Agrupado por dia com total diário; filtros por período, área e matéria |
| `/sessions/new`, `/sessions/:id` | Formulário | Data (padrão hoje), matéria agrupada por área, tópico filtrado, duração **ou** início/fim, tipo de atividade, foco em 5 botões, observações |
| `/planning` | Planejamento | Grade seg–dom com itens e total planejado × realizado por dia; navegação entre semanas; aba "Semana-modelo" com a mesma grade |
| `/catalog` | Catálogo | Desktop: 3 colunas (Áreas / Matérias / Tópicos); mobile: navegação em níveis; progresso do tópico editável na lista |

**Responsivo:** a partir de 768px, sidebar; abaixo, bottom navigation com Dashboard, Sessões, **+**, Planejamento, Catálogo. Indicadores em 4 colunas no desktop e 2×2 no mobile.

---

## 11. Ordem de construção (fatias verticais)

| Fatia | Entrega | Backend | Frontend |
|---|---|---|---|
| 0 — Fundação | Repositório no GitHub, banco no Docker, apps vazios rodando | Projeto Spring, conexão com o Postgres, Flyway ativo | Projeto Angular, Shell com navegação, tema, proxy |
| 1 — Áreas | CRUD de áreas ponta a ponta | V1, V2, entidade → repository → service → controller, erros globais, testes | `AreaApi`, tela de áreas no Catálogo |
| 2 — Matérias | CRUD de matérias | V3, mesmo padrão + filtros | Coluna de matérias |
| 3 — Tópicos | Tópicos e progresso | V4, progresso da matéria (R5) | Coluna de tópicos |
| 4 — Sessões | Registrar e listar estudo | V5, R6–R9 | Formulário e histórico |
| 5 — Planejamento | Semana-modelo e semana concreta | V6, R10–R14 | Grade semanal |
| 6 — Dashboard | Métricas e gráficos | Consultas agregadas, streak | Dashboard, period-picker, Chart.js |
| 7 — PWA e acabamento | App instalável, revisão de responsividade | — | `@angular/pwa`, ícones |

Cada fatia é uma **branch** (`feat/areas`, …) que termina em **Pull Request** no GitHub, revisado e mergeado na `main`.

---

## 12. Método de ensino

Para cada fatia (regra 29 do documento original):

1. Explicar o que será construído.
2. Explicar a arquitetura envolvida.
3. Listar os arquivos.
4. Implementar — **o usuário digita o código no IntelliJ**.
5. Explicar as decisões ("por que estamos fazendo dessa maneira?").
6. Executar e testar.
7. Corrigir problemas.
8. Só então avançar.

**Ajuda decrescente:**

- **1ª vez** que um padrão aparece (Fatia 1): código mostrado e explicado linha a linha.
- **2ª vez** (Fatia 2): esqueleto com lacunas para o usuário completar.
- **3ª vez em diante** (Fatia 3+): descrição do que fazer; o usuário escreve e o Claude revisa.

**Git evolui junto:** Fatia 0 — `init`, `.gitignore`, primeiro commit, repositório remoto, primeira branch, commits com Conventional Commits (`feat:`, `fix:`, `test:`, `docs:`), primeiro PR e merge; Fatia 1 em diante — o mesmo ciclo com cada vez menos ajuda; fatias seguintes — revisão do próprio PR, `git log`/`diff`, resolver conflitos quando surgirem.

---

## 13. Critérios de aceite do MVP

- [ ] `docker compose up -d` + backend + `ng serve` sobem sem erros em uma máquina limpa, seguindo o README.
- [ ] Todas as migrations aplicam do zero; `ddl-auto=validate` passa.
- [ ] Todos os endpoints da seção 8 existem e respeitam os erros definidos.
- [ ] R1–R14 cobertas por testes automatizados.
- [ ] Uma semana real de estudos registrada no app produz os mesmos totais da planilha.
- [ ] Dashboard correto nos 5 períodos e na comparação com o período anterior.
- [ ] Usável no celular (bottom nav) e instalável como PWA.
- [ ] Todo o código em `main` chegou via Pull Request (exceto o commit inicial com docs e `.gitignore`).
