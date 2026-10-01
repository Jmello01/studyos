# Fatia 0 — Fundação: Plano de Implementação

> **Método de execução:** desenvolvimento **guiado**. O usuário digita cada comando e cada arquivo no IntelliJ/terminal; o Claude explica, acompanha e revisa (spec, seção 12). Os passos usam checkbox (`- [ ]`) para acompanhamento.

**Objetivo:** ter o repositório no GitHub, o PostgreSQL no Docker, o backend Spring Boot conectado ao banco e o frontend Angular com o layout base (navegação + tema claro/escuro) rodando, ainda sem nenhuma funcionalidade de negócio.

**Arquitetura:** monorepo `studyos/` com `backend/` (Spring Boot 4.1, Maven Wrapper, Java 21) e `frontend/` (Angular 22). O PostgreSQL 17 roda via Docker Compose na porta **5433** do host. O Angular repassa `/api` e `/actuator` ao backend por proxy de desenvolvimento.

**Stack:** Git + GitHub · Docker Compose · PostgreSQL 17 · Spring Boot 4.1.1 (Web, Data JPA, Validation, Flyway, PostgreSQL, Testcontainers, DevTools, Actuator) · Angular 22 (standalone, signals, SCSS)

**Spec:** `docs/specs/2026-09-28-studyos-fase1-mvp-design.md`

## Restrições globais

- Projeto em `C:\Projetos\studyos` (fora do OneDrive).
- Java 21; Spring Boot 4.1.x; Angular 22; PostgreSQL 17.
- Banco no host em `localhost:5433` (a 5432 já é usada por um PostgreSQL local do Windows).
- Pacote Java base: `com.studyos`.
- Schema só via Flyway; `spring.jpa.hibernate.ddl-auto=validate`.
- Sem Lombok, sem MapStruct, sem biblioteca de componentes visuais.
- `.env` nunca é commitado; `.env.example` sim.
- Commits no padrão Conventional Commits (`feat:`, `chore:`, `docs:`, `test:`).
- Pré-requisito em toda sessão de trabalho: **Docker Desktop aberto**.

## Pontos de atenção na revisão

1. **Porta 5432 ocupada** pelo PostgreSQL do Windows: se o compose usar 5432, o backend conecta no banco errado ou o container não sobe. Esperado: container em 5433 e health do backend mostrando o banco `UP` (Task 3, passo 6).
2. **Docker Desktop fechado**: o teste com Testcontainers falha com erro obscuro ("Could not find a valid Docker environment"). Esperado: saber reconhecer a mensagem (Task 3, passo 3).
3. **Valor inválido salvo no `localStorage`** (ex.: `"roxo"`): o app deve cair para um tema válido, sem quebrar (Task 5, teste `ignora valor inválido salvo`).
4. **`window.matchMedia` indisponível** (ambiente de teste, navegadores antigos): sem valor salvo, o tema deve ser `light`, sem exceção (Task 5, teste `sem valor salvo e sem matchMedia, usa light`).
5. **Bottom nav cobrindo o conteúdo no celular**: o último item da página precisa continuar visível acima da barra (Task 5, verificação manual em 375px).

---

## Estrutura de arquivos ao final da Fatia 0

```
studyos/
├── .gitignore                         Task 1
├── README.md                          Task 1 (criado), Task 6 (completo)
├── docs/specs/…, docs/plans/…         já existem
├── docker-compose.yml                 Task 2
├── .env.example                       Task 2
├── .env                               Task 2 (NÃO vai para o Git)
├── backend/                           Task 3 (gerado pelo start.spring.io)
│   ├── pom.xml, mvnw, mvnw.cmd, .mvn/
│   └── src/
│       ├── main/java/com/studyos/StudyosApplication.java
│       ├── main/resources/application.yaml
│       ├── main/resources/db/migration/.gitkeep
│       └── test/java/com/studyos/{StudyosApplicationTests, TestcontainersConfiguration, TestStudyosApplication}.java
└── frontend/                          Task 4 (gerado pelo ng new), Task 5 (layout)
    ├── proxy.conf.json
    ├── angular.json                   (proxyConfig)
    └── src/
        ├── styles.scss                design tokens + tema
        └── app/
            ├── app.ts, app.spec.ts, app.routes.ts
            ├── core/layout/theme-service.ts (+ .spec.ts)
            ├── core/layout/shell/shell.{ts,html,scss}
            └── features/{dashboard,sessions,planning,catalog}/*-page.ts
```

---

### Task 1: Repositório Git local e remoto

**Conceitos:** o que é um repositório, `.gitignore`, commit, repositório remoto, `origin`, `push -u`.

**Arquivos:**
- Criar: `.gitignore`, `README.md`

**Interfaces:**
- Produz: branch `main` no GitHub (`origin/main`) com docs + `.gitignore` + README.

- [ ] **Passo 1: Conferir a identidade do Git**

```bash
git config --global user.name
git config --global user.email
```

Esperado: seu nome e e-mail. Esse é o autor que aparece em cada commit.

- [ ] **Passo 2: Inicializar o repositório com a branch `main`**

No terminal, dentro de `C:\Projetos\studyos`:

```bash
git init -b main
git status
```

Esperado: `On branch main`, `No commits yet` e `docs/` listado como *untracked*.

- [ ] **Passo 3: Criar `.gitignore` na raiz**

```gitignore
# Sistema operacional / IDE
.DS_Store
Thumbs.db
.idea/
*.iml
.vscode/

# Segredos locais
.env

# Backend (Maven)
backend/target/

# Frontend (Angular)
frontend/node_modules/
frontend/dist/
frontend/.angular/
```

*Por quê:* o Git só deve guardar o que é **fonte**. Pastas geradas (`target/`, `node_modules/`) são recriadas por comando, e o `.env` contém senhas.

- [ ] **Passo 4: Criar `README.md` inicial**

```markdown
# StudyOS

Sistema pessoal de gestão, planejamento e análise de estudos.

**Stack:** Java 21 · Spring Boot · Angular · PostgreSQL · Docker

- Spec do MVP: [docs/specs/2026-09-28-studyos-fase1-mvp-design.md](docs/specs/2026-09-28-studyos-fase1-mvp-design.md)
- Planos: [docs/plans/](docs/plans/)
```

- [ ] **Passo 5: Primeiro commit**

```bash
git add .
git status
git commit -m "docs: adiciona spec do MVP, plano da fatia 0 e gitignore"
git log --oneline
```

Esperado: 1 commit no log. Sempre rode `git status` **antes** do commit para conferir o que vai entrar.

- [ ] **Passo 6: Criar o repositório no GitHub (pelo site)**

Em github.com → **New repository**:
- Nome: `studyos`
- Visibilidade: **Public** (é portfólio)
- **Não** marque "Add a README", ".gitignore" nem "license". O repositório precisa nascer vazio, senão o primeiro push conflita com o histórico local.

- [ ] **Passo 7: Conectar e enviar**

```bash
git remote add origin https://github.com/<SEU_USUARIO>/studyos.git
git remote -v
git push -u origin main
```

Esperado: o Git Credential Manager abre o navegador para login na primeira vez. Depois do push, os arquivos aparecem no GitHub. O `-u` liga a `main` local à `origin/main`, e os próximos `git push` não precisam de argumentos.

---

### Task 2: PostgreSQL no Docker

**Conceitos:** imagem × container, volume, mapeamento de porta, variáveis de ambiente, healthcheck, branch de funcionalidade.

**Arquivos:**
- Criar: `docker-compose.yml`, `.env.example`, `.env`

**Interfaces:**
- Produz: banco `studyos`, usuário `studyos`, senha `studyos_dev`, em `localhost:5433`. O backend (Task 3) depende exatamente desses valores.

- [ ] **Passo 1: Criar a branch da fatia**

```bash
git switch -c feat/fundacao
git branch
```

Esperado: `* feat/fundacao`. Daqui até a Task 6, todo commit vai para essa branch. A `main` só recebe código via Pull Request.

- [ ] **Passo 2: Criar `.env.example` (vai para o Git)**

```dotenv
DB_NAME=studyos
DB_USER=studyos
DB_PASSWORD=studyos_dev
DB_PORT=5433
```

- [ ] **Passo 3: Criar `.env` (NÃO vai para o Git) copiando o exemplo**

```bash
copy .env.example .env
```

*Por quê dois arquivos?* O `.env.example` documenta **quais** variáveis existem; o `.env` guarda os **valores** da sua máquina. Em produção, a senha será outra e nunca estará no repositório.

- [ ] **Passo 4: Criar `docker-compose.yml`**

```yaml
services:
  db:
    image: postgres:17
    container_name: studyos-db
    environment:
      POSTGRES_DB: ${DB_NAME}
      POSTGRES_USER: ${DB_USER}
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    ports:
      - "${DB_PORT}:5432"
    volumes:
      - studyos-pgdata:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DB_USER} -d ${DB_NAME}"]
      interval: 5s
      timeout: 3s
      retries: 10

volumes:
  studyos-pgdata:
```

*Por quê:* `"5433:5432"` significa "porta 5433 do Windows → porta 5432 dentro do container". O **volume** nomeado guarda os dados fora do container, então apagar o container não apaga seus estudos.

- [ ] **Passo 5: Subir e verificar**

Com o Docker Desktop aberto:

```bash
docker compose up -d
docker compose ps
```

Esperado: `studyos-db` com status `running (healthy)` (pode levar alguns segundos até `healthy`).

- [ ] **Passo 6: Conversar com o banco pelo `psql` dentro do container**

```bash
docker compose exec db psql -U studyos -d studyos -c "select version();"
```

Esperado: `PostgreSQL 17.x …`.

- [ ] **Passo 7: Confirmar que o `.env` está ignorado e commitar**

```bash
git status
```

Esperado: aparecem `docker-compose.yml` e `.env.example`, e **não** aparece `.env`.

```bash
git add docker-compose.yml .env.example
git commit -m "chore: adiciona PostgreSQL 17 via Docker Compose"
```

---

### Task 3: Backend Spring Boot conectado ao banco

**Conceitos:** Spring Initializr, Maven Wrapper, `pom.xml`, starters, `application.yaml`, placeholders `${VAR:padrão}`, Flyway, Actuator, Testcontainers, rodar e depurar no IntelliJ.

**Arquivos:**
- Criar (gerado): `backend/**`
- Modificar: `backend/src/main/resources/application.yaml`
- Criar: `backend/src/main/resources/db/migration/.gitkeep`
- Teste: `backend/src/test/java/com/studyos/StudyosApplicationTests.java` (gerado)

**Interfaces:**
- Consome: banco da Task 2 (`localhost:5433`, `studyos` / `studyos` / `studyos_dev`).
- Produz: `GET http://localhost:8080/actuator/health` → `{"status":"UP", … "db":{"status":"UP"} …}`; classe `com.studyos.StudyosApplication`; pasta de migrations `db/migration` (a Fatia 1 cria `V1__create_areas.sql` ali).

- [ ] **Passo 1: Gerar o projeto em start.spring.io**

| Campo | Valor |
|---|---|
| Project | Maven |
| Language | Java |
| Spring Boot | 4.1.1 |
| Group | `com.studyos` |
| Artifact | `backend` |
| Name | `studyos` |
| Package name | `com.studyos` ← **edite**, o padrão seria `com.studyos.backend` |
| Packaging | Jar |
| Configuration | **YAML** |
| Java | 21 |

Dependências: **Spring Web**, **Spring Data JPA**, **Validation**, **Flyway Migration**, **PostgreSQL Driver**, **Testcontainers**, **Spring Boot DevTools**, **Spring Boot Actuator**.

Clique em **Explore** para ler o `pom.xml` antes de baixar. Depois, **Generate** e extraia o zip de modo que exista `C:\Projetos\studyos\backend\pom.xml`.

- [ ] **Passo 2: Abrir no IntelliJ**

**File → Open →** selecione `C:\Projetos\studyos\backend\pom.xml` → **Open as Project**. Aguarde o download das dependências (barra inferior). Em **File → Project Structure → SDK**, confirme o **21**.

- [ ] **Passo 3: Rodar o teste gerado ANTES de configurar (e ver o que acontece)**

O Initializr gerou `StudyosApplicationTests` (sobe a aplicação inteira) e `TestcontainersConfiguration` (sobe um PostgreSQL descartável no Docker só para o teste). No terminal, em `backend/`:

```bash
.\mvnw.cmd test
```

Esperado: `BUILD SUCCESS` e, no log, um container `postgres` sendo criado e destruído.

Se falhar com `Could not find a valid Docker environment`, o Docker Desktop está fechado. Abra e repita. (Esse é o ponto de atenção 2.)

*Por quê o teste passa sem configurar o banco?* O `@ServiceConnection` do Testcontainers injeta a URL do container de teste, ignorando o `application.yaml`. Os testes nunca tocam no seu banco de desenvolvimento.

- [ ] **Passo 4: Configurar `application.yaml`**

Substitua todo o conteúdo de `backend/src/main/resources/application.yaml`:

```yaml
spring:
  application:
    name: studyos
  datasource:
    url: jdbc:postgresql://localhost:${DB_PORT:5433}/${DB_NAME:studyos}
    username: ${DB_USER:studyos}
    password: ${DB_PASSWORD:studyos_dev}
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        jdbc:
          time_zone: UTC
  flyway:
    enabled: true

management:
  endpoints:
    web:
      exposure:
        include: health
  endpoint:
    health:
      show-details: always
```

*Por quê cada linha:*
- `${DB_PORT:5433}`: lê a variável de ambiente e, se não existir, usa `5433`. O IntelliJ não lê o `.env`, então os padrões espelham o `.env.example` (valores só de desenvolvimento).
- `ddl-auto: validate`: o Hibernate **nunca** cria tabelas; só confere se as entidades batem com o banco. Quem cria é o Flyway.
- `open-in-view: false`: desliga um atalho do Spring que mantém a conexão aberta durante a renderização da resposta e esconde problemas de performance.
- `time_zone: UTC`: grava `timestamptz` sempre em UTC.
- `show-details: always`: o health mostra *qual* componente falhou (só em dev; será revisto na Fase 4).

- [ ] **Passo 5: Criar a pasta de migrations versionável**

Crie o arquivo vazio `backend/src/main/resources/db/migration/.gitkeep`.
*Por quê:* o Git não versiona pastas vazias. O `.gitkeep` é uma convenção para manter a pasta.

- [ ] **Passo 6: Ver a aplicação FALHAR sem banco, depois funcionar**

```bash
docker compose stop
```

No IntelliJ, abra `StudyosApplication` e clique em ▶. Esperado: erro contendo `Connection to localhost:5433 refused`. Leia a stack trace de baixo para cima até achar a causa.

```bash
docker compose start
```

Rode de novo (▶). Esperado no log: `Tomcat started on port 8080` e `Started StudyosApplication`. O Flyway pode avisar que não há migrations, e isso é normal.

- [ ] **Passo 7: Verificar o health (navegador ou PowerShell)**

O IntelliJ Community não tem HTTP Client. Abra `http://localhost:8080/actuator/health` no navegador, ou rode `Invoke-RestMethod http://localhost:8080/actuator/health | ConvertTo-Json -Depth 5`. Esperado: `200` com `"status": "UP"` e `"db": { "status": "UP", "details": { "database": "PostgreSQL" … } }`. (Ponto de atenção 1: se você vir o banco `UP` mas errar a porta, estaria falando com o PostgreSQL do Windows. Confira que a URL no log é `:5433`.)

- [ ] **Passo 8: Rodar os testes de novo e commitar**

```bash
.\mvnw.cmd test
```

Esperado: `BUILD SUCCESS`.

```bash
cd ..
git status
git add backend
git commit -m "feat: cria backend Spring Boot conectado ao PostgreSQL"
```

Confira no `git status` que **não** aparece `backend/target/`.

---

### Task 4: Frontend Angular com proxy para o backend

**Conceitos:** Node/npm, Angular CLI, estrutura de um projeto Angular, `ng serve`, proxy de desenvolvimento.

**Arquivos:**
- Criar (gerado): `frontend/**`
- Criar: `frontend/proxy.conf.json`
- Modificar: `frontend/angular.json` (target `serve`)

**Interfaces:**
- Consome: backend em `localhost:8080`.
- Produz: `http://localhost:4200/actuator/health` respondendo via proxy. A partir da Fatia 1, `/api/**` segue o mesmo caminho.

- [ ] **Passo 1: Instalar a Angular CLI**

```bash
npm install -g @angular/cli@22
ng version
```

Esperado: `Angular CLI: 22.x`.

- [ ] **Passo 2: Gerar o projeto**

Em `C:\Projetos\studyos`:

```bash
ng new frontend --style=scss --ssr=false --skip-git
```

Se perguntar sobre ferramentas de IA, escolha **None**.
*Por quê `--skip-git`?* O repositório Git já existe na raiz. Sem essa flag, o Angular criaria um repositório **dentro** do outro.

- [ ] **Passo 3: Rodar e ver a página padrão**

```bash
cd frontend
ng serve
```

Abra `http://localhost:4200`. Esperado: a página de boas-vindas do Angular. Pare com `Ctrl+C`.

- [ ] **Passo 4: Criar `frontend/proxy.conf.json`**

```json
{
  "/api": {
    "target": "http://localhost:8080",
    "secure": false
  },
  "/actuator": {
    "target": "http://localhost:8080",
    "secure": false
  }
}
```

- [ ] **Passo 5: Ligar o proxy no `angular.json`**

Em `frontend/angular.json`, localize `projects → frontend → architect → serve`. Dentro dele, em `"options"` (crie o objeto se não existir), adicione:

```json
"options": {
  "proxyConfig": "proxy.conf.json"
}
```

*Por quê o proxy?* O navegador acha que tudo vem da porta 4200, então não há requisição entre origens diferentes e não é preciso configurar CORS. Em produção, frontend e backend ficarão atrás do mesmo endereço, e o proxy imita isso.

- [ ] **Passo 6: Verificar o proxy**

Backend rodando no IntelliJ + `ng serve` rodando. Abra `http://localhost:4200/actuator/health`.
Esperado: o mesmo JSON `UP` da Task 3, agora servido pela porta 4200.

- [ ] **Passo 7: Rodar os testes gerados**

```bash
ng test --watch=false
```

Esperado: todos os testes passam.

- [ ] **Passo 8: Commitar**

```bash
cd ..
git status
git add frontend
git commit -m "feat: cria frontend Angular com proxy para o backend"
```

Confira que **não** aparece `frontend/node_modules/`.

---

### Task 5: Layout base: navegação responsiva e tema claro/escuro

**Conceitos:** componente standalone, template com `@for`, rotas com lazy loading, `RouterLink`/`RouterLinkActive`, serviço com `inject()`, **signals** (`signal`, `update`, `effect`), design tokens em CSS, media query, TDD no Angular.

**Arquivos:**
- Modificar: `frontend/src/styles.scss`, `frontend/src/app/app.ts`, `frontend/src/app/app.spec.ts`, `frontend/src/app/app.routes.ts`
- Excluir: `frontend/src/app/app.html`, `frontend/src/app/app.scss`
- Criar: `frontend/src/app/core/layout/theme-service.ts`, `frontend/src/app/core/layout/theme-service.spec.ts`
- Criar: `frontend/src/app/core/layout/shell/shell.ts`, `shell.html`, `shell.scss`
- Criar: `frontend/src/app/features/dashboard/dashboard-page.ts`, `features/sessions/sessions-page.ts`, `features/planning/planning-page.ts`, `features/catalog/catalog-page.ts`

**Interfaces:**
- Produz:
  - `ThemeService` (`providedIn: 'root'`): `theme: WritableSignal<'light' | 'dark'>`, `toggle(): void`; grava em `localStorage['studyos.theme']` e em `<html data-theme="…">`.
  - `Shell` (seletor `app-shell`): layout com `<router-outlet />`.
  - Rotas: `/dashboard`, `/sessions`, `/planning`, `/catalog`; `''` e `**` redirecionam para `/dashboard`.
  - Tokens CSS em `:root`: `--bg`, `--surface`, `--surface-hover`, `--border`, `--text-primary`, `--text-secondary`, `--accent`, `--radius`, `--space-1…6`. As fatias seguintes usam esses nomes.

- [ ] **Passo 1: Escrever o teste do `ThemeService` (que ainda não existe)**

Crie `frontend/src/app/core/layout/theme-service.spec.ts`:

```ts
import { TestBed } from '@angular/core/testing';
import { ThemeService } from './theme-service';

describe('ThemeService', () => {
  beforeEach(() => {
    localStorage.clear();
    delete document.documentElement.dataset['theme'];
    TestBed.configureTestingModule({});
  });

  it('usa o tema salvo no localStorage', () => {
    localStorage.setItem('studyos.theme', 'dark');
    const service = TestBed.inject(ThemeService);
    expect(service.theme()).toBe('dark');
  });

  it('alterna entre claro e escuro', () => {
    localStorage.setItem('studyos.theme', 'light');
    const service = TestBed.inject(ThemeService);

    service.toggle();
    expect(service.theme()).toBe('dark');

    service.toggle();
    expect(service.theme()).toBe('light');
  });

  it('aplica o tema no <html> e salva a escolha', () => {
    localStorage.setItem('studyos.theme', 'light');
    const service = TestBed.inject(ThemeService);

    service.toggle();
    TestBed.tick();

    expect(document.documentElement.dataset['theme']).toBe('dark');
    expect(localStorage.getItem('studyos.theme')).toBe('dark');
  });

  it('ignora valor inválido salvo', () => {
    localStorage.setItem('studyos.theme', 'roxo');
    const service = TestBed.inject(ThemeService);
    expect(['light', 'dark']).toContain(service.theme());
  });

  it('sem valor salvo e sem matchMedia, usa light', () => {
    const original = window.matchMedia;
    Object.defineProperty(window, 'matchMedia', { value: undefined, configurable: true });

    const service = TestBed.inject(ThemeService);
    expect(service.theme()).toBe('light');

    Object.defineProperty(window, 'matchMedia', { value: original, configurable: true });
  });
});
```

- [ ] **Passo 2: Rodar e ver falhar**

```bash
cd frontend
ng test --watch=false
```

Esperado: FAIL, porque o arquivo `./theme-service` não existe. *Por quê ver falhar?* Prova que o teste realmente testa algo. Um teste que nunca falhou pode estar passando por engano.

- [ ] **Passo 3: Implementar `ThemeService`**

Crie `frontend/src/app/core/layout/theme-service.ts`:

```ts
import { DOCUMENT, Injectable, effect, inject, signal } from '@angular/core';

export type Theme = 'light' | 'dark';

const STORAGE_KEY = 'studyos.theme';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly document = inject(DOCUMENT);

  readonly theme = signal<Theme>(this.initialTheme());

  constructor() {
    effect(() => {
      const theme = this.theme();
      this.document.documentElement.dataset['theme'] = theme;
      localStorage.setItem(STORAGE_KEY, theme);
    });
  }

  toggle(): void {
    this.theme.update((current) => (current === 'dark' ? 'light' : 'dark'));
  }

  private initialTheme(): Theme {
    const saved = localStorage.getItem(STORAGE_KEY);
    if (saved === 'light' || saved === 'dark') {
      return saved;
    }
    const prefersDark = window.matchMedia?.('(prefers-color-scheme: dark)').matches ?? false;
    return prefersDark ? 'dark' : 'light';
  }
}
```

*Por quê:* `signal` guarda o estado; `effect` reage a **toda** mudança do signal e sincroniza o HTML e o `localStorage` num lugar só, sem precisar lembrar de salvar em cada método. `?.` e `??` protegem contra `matchMedia` inexistente.

- [ ] **Passo 4: Rodar e ver passar**

```bash
ng test --watch=false
```

Esperado: os 5 testes do `ThemeService` passam.

- [ ] **Passo 5: Design tokens globais**

Substitua `frontend/src/styles.scss`:

```scss
:root {
  --font-sans: system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif;

  --space-1: 0.25rem;
  --space-2: 0.5rem;
  --space-3: 0.75rem;
  --space-4: 1rem;
  --space-6: 1.5rem;
  --radius: 6px;

  --bg: #fafafa;
  --surface: #ffffff;
  --surface-hover: #f0f0f0;
  --border: #e5e5e5;
  --text-primary: #171717;
  --text-secondary: #525252;
  --accent: #2563eb;

  color-scheme: light;
}

:root[data-theme='dark'] {
  --bg: #0a0a0a;
  --surface: #141414;
  --surface-hover: #1f1f1f;
  --border: #262626;
  --text-primary: #ededed;
  --text-secondary: #a3a3a3;
  --accent: #3b82f6;

  color-scheme: dark;
}

*,
*::before,
*::after {
  box-sizing: border-box;
}

body {
  margin: 0;
  font-family: var(--font-sans);
  background: var(--bg);
  color: var(--text-primary);
  -webkit-font-smoothing: antialiased;
}
```

*Por quê tokens?* Os componentes nunca usam cores diretas como `#171717`, e sim `var(--text-primary)`. Trocar o tema é só trocar o valor das variáveis, e todos os componentes acompanham.

- [ ] **Passo 6: Páginas provisórias (uma por rota)**

Crie `frontend/src/app/features/dashboard/dashboard-page.ts`:

```ts
import { Component } from '@angular/core';

@Component({
  selector: 'app-dashboard-page',
  template: `<h1>Dashboard</h1><p>Em construção (Fatia 6).</p>`,
})
export class DashboardPage {}
```

Crie `frontend/src/app/features/sessions/sessions-page.ts`:

```ts
import { Component } from '@angular/core';

@Component({
  selector: 'app-sessions-page',
  template: `<h1>Sessões</h1><p>Em construção (Fatia 4).</p>`,
})
export class SessionsPage {}
```

Crie `frontend/src/app/features/planning/planning-page.ts`:

```ts
import { Component } from '@angular/core';

@Component({
  selector: 'app-planning-page',
  template: `<h1>Planejamento</h1><p>Em construção (Fatia 5).</p>`,
})
export class PlanningPage {}
```

Crie `frontend/src/app/features/catalog/catalog-page.ts`:

```ts
import { Component } from '@angular/core';

@Component({
  selector: 'app-catalog-page',
  template: `<h1>Catálogo</h1><p>Em construção (Fatias 1 a 3).</p>`,
})
export class CatalogPage {}
```

- [ ] **Passo 7: Rotas com lazy loading**

Substitua `frontend/src/app/app.routes.ts`:

```ts
import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
  {
    path: 'dashboard',
    loadComponent: () => import('./features/dashboard/dashboard-page').then((m) => m.DashboardPage),
  },
  {
    path: 'sessions',
    loadComponent: () => import('./features/sessions/sessions-page').then((m) => m.SessionsPage),
  },
  {
    path: 'planning',
    loadComponent: () => import('./features/planning/planning-page').then((m) => m.PlanningPage),
  },
  {
    path: 'catalog',
    loadComponent: () => import('./features/catalog/catalog-page').then((m) => m.CatalogPage),
  },
  { path: '**', redirectTo: 'dashboard' },
];
```

*Por quê `loadComponent` com `import()`?* Cada tela vira um arquivo JavaScript separado, baixado só quando você abre a tela (**lazy loading**). O app inicia mais rápido.

- [ ] **Passo 8: Escrever o teste do layout**

Substitua `frontend/src/app/app.spec.ts`:

```ts
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { App } from './app';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter([])],
    }).compileComponents();
  });

  it('renderiza o shell com os 4 itens de navegação', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    const links: NodeListOf<HTMLAnchorElement> =
      fixture.nativeElement.querySelectorAll('a.nav-link');
    const labels = Array.from(links).map((a) => a.textContent?.trim());

    expect(labels).toEqual(['Dashboard', 'Sessões', 'Planejamento', 'Catálogo']);
  });

  it('tem um botão para alternar o tema', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    const button: HTMLButtonElement | null =
      fixture.nativeElement.querySelector('button.theme-toggle');

    expect(button).not.toBeNull();
  });
});
```

- [ ] **Passo 9: Rodar e ver falhar**

```bash
ng test --watch=false
```

Esperado: FAIL nos 2 testes de `App`. O componente ainda mostra a página padrão, sem `a.nav-link`.

- [ ] **Passo 10: Criar o `Shell`**

`frontend/src/app/core/layout/shell/shell.ts`:

```ts
import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { ThemeService } from '../theme-service';

interface NavItem {
  path: string;
  label: string;
}

@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './shell.html',
  styleUrl: './shell.scss',
})
export class Shell {
  protected readonly themeService = inject(ThemeService);

  protected readonly navItems: NavItem[] = [
    { path: '/dashboard', label: 'Dashboard' },
    { path: '/sessions', label: 'Sessões' },
    { path: '/planning', label: 'Planejamento' },
    { path: '/catalog', label: 'Catálogo' },
  ];
}
```

`frontend/src/app/core/layout/shell/shell.html`:

```html
<div class="shell">
  <aside class="sidebar">
    <span class="brand">StudyOS</span>

    <nav class="nav" aria-label="Navegação principal">
      @for (item of navItems; track item.path) {
        <a class="nav-link" [routerLink]="item.path" routerLinkActive="active">{{ item.label }}</a>
      }
    </nav>

    <button type="button" class="theme-toggle" (click)="themeService.toggle()">
      {{ themeService.theme() === 'dark' ? 'Tema claro' : 'Tema escuro' }}
    </button>
  </aside>

  <main class="content">
    <router-outlet />
  </main>
</div>
```

`frontend/src/app/core/layout/shell/shell.scss`:

```scss
.shell {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  min-height: 100dvh;
}

.sidebar {
  position: sticky;
  top: 0;
  height: 100dvh;
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  padding: var(--space-4) var(--space-3);
  border-right: 1px solid var(--border);
  background: var(--surface);
}

.brand {
  font-weight: 600;
  padding: 0 var(--space-2);
  margin-bottom: var(--space-4);
}

.nav {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex: 1;
}

.nav-link {
  padding: var(--space-2);
  border-radius: var(--radius);
  color: var(--text-secondary);
  text-decoration: none;
  font-size: 0.875rem;

  &:hover,
  &.active {
    background: var(--surface-hover);
    color: var(--text-primary);
  }
}

.theme-toggle {
  padding: var(--space-2);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  background: transparent;
  color: var(--text-secondary);
  font: inherit;
  font-size: 0.8125rem;
  cursor: pointer;

  &:hover {
    color: var(--text-primary);
  }
}

.content {
  padding: var(--space-6);
}

@media (max-width: 767px) {
  .shell {
    grid-template-columns: minmax(0, 1fr);
  }

  .sidebar {
    position: static;
    height: auto;
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
    padding: var(--space-3) var(--space-4);
    border-right: 0;
    border-bottom: 1px solid var(--border);
  }

  .brand {
    margin: 0;
  }

  .nav {
    position: fixed;
    left: 0;
    right: 0;
    bottom: 0;
    z-index: 10;
    flex-direction: row;
    justify-content: space-around;
    padding: var(--space-2) var(--space-2) calc(var(--space-2) + env(safe-area-inset-bottom));
    border-top: 1px solid var(--border);
    background: var(--surface);
  }

  .content {
    padding: var(--space-4) var(--space-4) 5rem;
  }
}
```

*Por quê uma única `<nav>` para desktop e mobile?* O mesmo HTML muda de posição só com CSS (media query). Há uma lista de links só, e nenhum risco de as duas navegações ficarem diferentes. O `padding-bottom: 5rem` do conteúdo no mobile evita que a barra fixa cubra o fim da página (ponto de atenção 5).

- [ ] **Passo 11: Fazer o `App` usar o `Shell`**

Exclua `frontend/src/app/app.html` e `frontend/src/app/app.scss`. Substitua `frontend/src/app/app.ts`:

```ts
import { Component } from '@angular/core';
import { Shell } from './core/layout/shell/shell';

@Component({
  selector: 'app-root',
  imports: [Shell],
  template: '<app-shell />',
})
export class App {}
```

- [ ] **Passo 12: Rodar e ver passar**

```bash
ng test --watch=false
```

Esperado: todos os testes passam (5 do `ThemeService` + 2 do `App`).

- [ ] **Passo 13: Verificação visual**

`ng serve` → `http://localhost:4200`:
1. Abre em `/dashboard`, e o link "Dashboard" aparece destacado.
2. Clicar nos 4 links troca a página, e a URL muda.
3. "Tema escuro" troca as cores. Recarregue a página (F5): o tema escolhido é mantido.
4. Acesse `http://localhost:4200/qualquer-coisa`: redireciona para `/dashboard`.
5. DevTools (F12) → modo dispositivo → largura **375px**: a navegação vai para o rodapé, e o texto da página não fica escondido atrás dela.

- [ ] **Passo 14: Commitar**

```bash
cd ..
git status
git add frontend
git commit -m "feat: adiciona layout base com navegação responsiva e tema claro/escuro"
```

---

### Task 6: README de execução e primeiro Pull Request

**Conceitos:** documentação de onboarding, push de branch, Pull Request, revisão de diff, squash merge, sincronizar a `main` local.

**Arquivos:**
- Modificar: `README.md`

**Interfaces:**
- Produz: `main` no GitHub contendo toda a Fatia 0; branch `feat/fundacao` removida.

- [ ] **Passo 1: Completar o README**

Substitua `README.md`:

````markdown
# StudyOS

Sistema pessoal de gestão, planejamento e análise de estudos.

**Stack:** Java 21 · Spring Boot 4 · Angular 22 · PostgreSQL 17 · Docker

## Pré-requisitos

- Java 21
- Node.js 24+ e Angular CLI 22 (`npm install -g @angular/cli@22`)
- Docker Desktop (aberto)

## Como rodar

```bash
# 1. Banco (porta 5433)
copy .env.example .env
docker compose up -d

# 2. Backend (porta 8080) — ou ▶ em StudyosApplication no IntelliJ
cd backend
.\mvnw.cmd spring-boot:run

# 3. Frontend (porta 4200)
cd frontend
npm install
ng serve
```

Acesse http://localhost:4200. Saúde do backend: http://localhost:4200/actuator/health

## Testes

```bash
cd backend && .\mvnw.cmd test      # requer Docker aberto (Testcontainers)
cd frontend && ng test --watch=false
```

## Documentação

- Spec do MVP: [docs/specs/2026-09-28-studyos-fase1-mvp-design.md](docs/specs/2026-09-28-studyos-fase1-mvp-design.md)
- Planos por fatia: [docs/plans/](docs/plans/)
````

- [ ] **Passo 2: Commitar e enviar a branch**

```bash
git add README.md
git commit -m "docs: adiciona instruções de execução ao README"
git log --oneline
git push -u origin feat/fundacao
```

Esperado: 5 commits no log (1 da `main` + 4 da branch).

- [ ] **Passo 3: Abrir o Pull Request no GitHub**

No GitHub aparece o aviso "feat/fundacao had recent pushes" → **Compare & pull request**.
- Base: `main` ← Compare: `feat/fundacao`
- Título: `Fatia 0: fundação do projeto`
- Descrição: o que foi feito (banco, backend, frontend, layout) e como testar (copie a seção "Como rodar").

- [ ] **Passo 4: Revisar o próprio PR**

Aba **Files changed**: leia o diff como se fosse de outra pessoa. Confira que não entrou `.env`, `target/` nem `node_modules/`.

- [ ] **Passo 5: Merge**

**Squash and merge** → confirme.
*Por quê squash?* Os 4 commits da branch viram 1 commit na `main` ("Fatia 0: fundação do projeto"). A `main` fica com um commit por funcionalidade, fácil de ler, e os commits detalhados continuam visíveis no PR.

- [ ] **Passo 6: Sincronizar a máquina**

```bash
git switch main
git pull
git log --oneline
git branch -D feat/fundacao
```

Esperado: `main` com 2 commits (inicial + fatia 0). O `-D` (maiúsculo) é necessário porque, após um squash, o Git não reconhece os commits da branch como "mergeados", mesmo que o conteúdo já esteja na `main`.

- [ ] **Passo 7: Checklist de conclusão da Fatia 0**

- [ ] `docker compose ps` → `studyos-db` healthy
- [ ] `.\mvnw.cmd test` → BUILD SUCCESS
- [ ] `ng test --watch=false` → 7 testes passando
- [ ] `http://localhost:4200/actuator/health` → `UP` com `db` `UP`
- [ ] Navegação, tema e layout mobile funcionando
- [ ] GitHub: `main` atualizada, PR mergeado, sem `.env` no repositório
