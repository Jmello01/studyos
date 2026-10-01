# StudyOS

Sistema pessoal de gestão, planejamento e análise de estudos.

**Stack:** Java 21 · Spring Boot 4 · Angular 22 · PostgreSQL 17 · Docker

## Pré-requisitos

- Java 21
- Node.js 24+ e Angular CLI 22 (`npm install -g @angular/cli@22`)
- Docker Desktop

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