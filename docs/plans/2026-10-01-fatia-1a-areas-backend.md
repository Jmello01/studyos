# Fatia 1A — Áreas (backend): Plano de Implementação

> **Método de execução:** desenvolvimento **guiado**. O usuário digita cada arquivo no IntelliJ e roda os comandos; o Claude explica o porquê, confere a saída e revisa. Primeira vez que o padrão Controller → Service → Repository aparece: o código é mostrado e explicado por inteiro. Os passos usam checkbox (`- [ ]`).

**Objetivo:** CRUD de Áreas ponta a ponta no backend (migrations, entidade, repository, service, controller, tratamento de erros), com testes em três níveis e as 9 áreas iniciais semeadas no banco.

**Arquitetura:** `AreaController` (HTTP) → `AreaService` (regras) → `AreaRepository` (Spring Data JPA) → tabela `areas` (Flyway). A entidade `Area` nunca sai da API: o controller recebe `AreaRequest` e devolve `AreaResponse` (records), convertidos pelo `AreaMapper`. Erros viram `ProblemDetail` pelo `GlobalExceptionHandler`.

**Stack:** Spring Boot 4.1.1 · Java 21 · Spring Data JPA/Hibernate · Flyway · Bean Validation · JUnit 5 · Mockito · AssertJ · Testcontainers (PostgreSQL 17)

**Spec:** `docs/specs/2026-09-28-studyos-fase1-mvp-design.md` (seções 5.1, 5.8, 6 R1–R4, 8, 9)

## Restrições globais

- Pacote base `com.studyos`; camadas `controller`, `service`, `repository`, `entity`, `dto`, `mapper`, `exception`.
- Entidade **nunca** vira JSON; DTOs são `record`; mapper escrito à mão (sem MapStruct, sem Lombok).
- Controller sem regra de negócio e sem acesso ao repository; service sem conhecimento de HTTP.
- Schema só via Flyway (`ddl-auto: validate`); migration aplicada **nunca é editada**.
- Áreas **não têm `DELETE`**: só `archive` / `unarchive` (R2).
- Nome da área único **sem diferenciar maiúsculas** (R1) → `409 Conflict`.
- Erros no formato `ProblemDetail`: `400` validação (com `errors` por campo), `404` inexistente, `409` duplicado.
- Testes de banco usam `postgres:17` (a mesma versão do Docker Compose).
- Commits em Conventional Commits; todo o trabalho em branch `feat/areas-backend` e entra na `main` por Pull Request.
- Pré-requisito: **Docker Desktop aberto** (Testcontainers).
- Código validado em rascunho: a suíte completa (28 testes) passa com Spring Boot 4.1.1.

## Pontos de atenção na revisão

1. **Nome só com espaços** (`"   "`) deve virar `400` com `errors.name`, não ser salvo nem causar `500` (teste `nomeEmBrancoDevolve400ComErroPorCampo`).
2. **Mesmo nome com outra caixa** (`"idiomas extras"` quando existe `"Idiomas Extras"`) deve dar `409`, não criar duplicata (teste de integração `fluxoCompletoDeUmaArea`).
3. **Editar uma área mantendo o próprio nome** deve funcionar (`200`), sem conflitar consigo mesma (teste de integração e `existsByNameIgnoreCaseAndIdNotIgnoraOProprioRegistro`).
4. **Acentos na migration** (`INGLÊS`) devem chegar ao banco corretos: o arquivo SQL precisa estar salvo em UTF-8 (teste `migracaoSemeiaAsNoveAreasIniciais`).
5. **JSON malformado e `id` não numérico** (`/api/areas/abc`) devem dar `400`, nunca `500` (testes `jsonMalFormadoDevolve400` e `idNaoNumericoDevolve400`).

---

## Estrutura de arquivos ao final da Fatia 1A

```
backend/src/main/
├── java/com/studyos/
│   ├── controller/AreaController.java
│   ├── service/AreaService.java
│   ├── repository/AreaRepository.java
│   ├── entity/Area.java
│   ├── dto/AreaRequest.java, AreaResponse.java
│   ├── mapper/AreaMapper.java
│   └── exception/ResourceNotFoundException.java, ConflictException.java, GlobalExceptionHandler.java
└── resources/db/migration/V1__create_areas.sql, V2__seed_areas.sql

backend/src/test/java/com/studyos/
├── TestcontainersConfiguration.java        (modificado)
├── AreaApiIntegrationTest.java
├── repository/AreaRepositoryTest.java
├── service/AreaServiceTest.java
└── controller/AreaControllerTest.java
```

---

### Task 1: Branch e migrations

**Conceitos:** branch de funcionalidade, Flyway (nome `V<n>__descricao.sql`), `flyway_schema_history`, por que migration não se edita, índice único funcional `lower(name)`, constraint `CHECK`.

**Arquivos:**
- Criar: `backend/src/main/resources/db/migration/V1__create_areas.sql`, `V2__seed_areas.sql`
- Modificar (já pendente): `docs/specs/2026-09-28-studyos-fase1-mvp-design.md`

**Interfaces:**
- Produz: tabela `areas(id, name, description, color, icon, position, archived, created_at, updated_at)` com 9 linhas semeadas nas posições 1 a 9. A Task 2 mapeia exatamente essas colunas.

- [ ] **Passo 1: Criar a branch**

```bash
git switch main
git pull
git switch -c feat/areas-backend
```

- [ ] **Passo 2: Commitar a alteração pendente da spec**

A spec tem alterações locais (item "README vitrine" na Fatia 7 e a regra do nome único sem diferenciar maiúsculas). Veja o que mudou antes de commitar:

```bash
git diff --stat
git add docs
git commit -m "docs: registra README vitrine na fatia 7 e unicidade de nome sem caixa"
```

- [ ] **Passo 3: Criar `backend/src/main/resources/db/migration/V1__create_areas.sql`**

```sql
CREATE TABLE areas (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    color       VARCHAR(7),
    icon        VARCHAR(50),
    position    INT          NOT NULL DEFAULT 0,
    archived    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_areas_color CHECK (color IS NULL OR color ~ '^#[0-9A-Fa-f]{6}$')
);

CREATE UNIQUE INDEX uq_areas_name_lower ON areas (lower(name));
```

*Por quê:* `GENERATED ALWAYS AS IDENTITY` é o jeito moderno do PostgreSQL de auto-incrementar. O índice em `lower(name)` faz o **banco** impedir "Cloud" e "CLOUD" juntos, e é a rede de segurança caso o service falhe em checar. O `CHECK` garante o formato `#RRGGBB` mesmo que alguém insira direto no banco.

- [ ] **Passo 4: Criar `V2__seed_areas.sql` (mesma pasta)**

```sql
INSERT INTO areas (name, position) VALUES
    ('FACULDADE', 1),
    ('FULL STACK', 2),
    ('INGLÊS', 3),
    ('ALGORITMOS', 4),
    ('BANCO DE DADOS', 5),
    ('CLOUD', 6),
    ('PROJETOS', 7),
    ('CARREIRA', 8),
    ('LEITURA', 9);
```

*Por que um arquivo separado do V1?* Estrutura e dados iniciais mudam por motivos diferentes. Salve o arquivo em **UTF-8** (o IntelliJ usa UTF-8 por padrão; confira no rodapé do editor), por causa do `Ê`.

- [ ] **Passo 5: Rodar a aplicação e conferir no banco**

Docker Compose no ar (`docker compose ps`), depois ▶ em `StudyosApplication`. No log deve aparecer `Migrating schema "public" to version "1 - create areas"` e `"2 - seed areas"`. Depois:

```bash
docker compose exec db psql -U studyos -d studyos -c "select version, description, success from flyway_schema_history;"
docker compose exec db psql -U studyos -d studyos -c "select id, name, position from areas order by position;"
```

Esperado: 2 linhas no histórico (`success = t`) e 9 áreas. (O console do Windows pode exibir `INGLÊS` com caracteres estranhos: é só o terminal. O teste da Task 2 confere o valor real.)

- [ ] **Passo 6: Commit**

```bash
git add backend
git commit -m "feat: adiciona migrations da tabela de areas e seed inicial"
```

---

### Task 2: Entidade e repository (com testes em PostgreSQL real)

**Conceitos:** entidade JPA (`@Entity`, `@Id`, `@Column`), `@PrePersist`/`@PreUpdate`, Spring Data (consultas pelo **nome do método**), `@Query` (JPQL), `@DataJpaTest`, Testcontainers.

**Arquivos:**
- Modificar: `backend/src/test/java/com/studyos/TestcontainersConfiguration.java`
- Criar: `backend/src/main/java/com/studyos/entity/Area.java`, `backend/src/main/java/com/studyos/repository/AreaRepository.java`
- Teste: `backend/src/test/java/com/studyos/repository/AreaRepositoryTest.java`

**Interfaces:**
- Produz: `Area` (getters/setters; `getId(): Long`, `getName()`, `getDescription()`, `getColor()`, `getIcon()`, `getPosition(): int`, `isArchived(): boolean`, `getCreatedAt()/getUpdatedAt(): Instant`).
- Produz: `AreaRepository extends JpaRepository<Area, Long>` com `List<Area> findAllByOrderByPositionAscNameAsc()`, `List<Area> findByArchivedFalseOrderByPositionAscNameAsc()`, `boolean existsByNameIgnoreCase(String)`, `boolean existsByNameIgnoreCaseAndIdNot(String, Long)`, `int findMaxPosition()`.

- [ ] **Passo 1: Ajustar `TestcontainersConfiguration`**

O arquivo gerado usa `postgres:latest` (pode ser uma versão diferente da real) e é visível só no próprio pacote. Duas mudanças em `backend/src/test/java/com/studyos/TestcontainersConfiguration.java`: trocar `class TestcontainersConfiguration` por `public class TestcontainersConfiguration` e `"postgres:latest"` por `"postgres:17"`. Resultado:

```java
package com.studyos;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	@Bean
	@ServiceConnection
	PostgreSQLContainer postgresContainer() {
		return new PostgreSQLContainer(DockerImageName.parse("postgres:17"));
	}

}
```

- [ ] **Passo 2: Escrever o teste do repository (a entidade ainda não existe)**

Crie `backend/src/test/java/com/studyos/repository/AreaRepositoryTest.java`:

```java
package com.studyos.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.studyos.TestcontainersConfiguration;
import com.studyos.entity.Area;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class AreaRepositoryTest {

    @Autowired
    private AreaRepository repository;

    private Area newArea(String name, int position) {
        Area area = new Area();
        area.setName(name);
        area.setPosition(position);
        return area;
    }

    @Test
    void migracaoSemeiaAsNoveAreasIniciais() {
        List<Area> areas = repository.findAllByOrderByPositionAscNameAsc();

        assertThat(areas).hasSize(9);
        assertThat(areas.get(0).getName()).isEqualTo("FACULDADE");
        assertThat(areas.get(2).getName()).isEqualTo("INGLÊS");
        assertThat(areas.get(8).getName()).isEqualTo("LEITURA");
    }

    @Test
    void salvaAreaPreenchendoIdETimestamps() {
        Area saved = repository.saveAndFlush(newArea("Estudos Livres", 10));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.isArchived()).isFalse();
    }

    @Test
    void existsByNameIgnoreCaseEncontraNomeComOutraCaixa() {
        assertThat(repository.existsByNameIgnoreCase("faculdade")).isTrue();
        assertThat(repository.existsByNameIgnoreCase("Inexistente")).isFalse();
    }

    @Test
    void existsByNameIgnoreCaseAndIdNotIgnoraOProprioRegistro() {
        Area faculdade = repository.findAllByOrderByPositionAscNameAsc().get(0);

        assertThat(repository.existsByNameIgnoreCaseAndIdNot("FACULDADE", faculdade.getId())).isFalse();
        assertThat(repository.existsByNameIgnoreCaseAndIdNot("FACULDADE", -1L)).isTrue();
    }

    @Test
    void bancoRejeitaNomeDuplicadoMesmoComCaixaDiferente() {
        assertThatThrownBy(() -> repository.saveAndFlush(newArea("faculdade", 10)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void listagemSemArquivadasOcultaAsArquivadas() {
        Area cloud = repository.findAllByOrderByPositionAscNameAsc().stream()
                .filter(a -> a.getName().equals("CLOUD"))
                .findFirst()
                .orElseThrow();
        cloud.setArchived(true);
        repository.saveAndFlush(cloud);

        List<String> ativas = repository.findByArchivedFalseOrderByPositionAscNameAsc().stream()
                .map(Area::getName)
                .toList();

        assertThat(ativas).hasSize(8).doesNotContain("CLOUD");
    }

    @Test
    void findMaxPositionDevolveAMaiorPosicao() {
        assertThat(repository.findMaxPosition()).isEqualTo(9);
    }
}
```

*Como ler:* `@DataJpaTest` sobe só a parte de persistência (rápido). `@AutoConfigureTestDatabase(replace = NONE)` diz "**não** troque meu banco por um embutido": queremos o PostgreSQL real do Testcontainers. Cada teste roda numa transação que é **desfeita** no fim, então um teste não suja o outro. Como o Flyway roda ao subir, o banco do teste **já tem as 9 áreas** semeadas.

- [ ] **Passo 3: Rodar e ver falhar**

```bash
cd backend
.\mvnw.cmd test "-Dtest=AreaRepositoryTest"
```

Esperado: erro de compilação (`cannot find symbol: class Area` / `AreaRepository`). As aspas em `"-Dtest=..."` são necessárias no PowerShell.

- [ ] **Passo 4: Criar `backend/src/main/java/com/studyos/entity/Area.java`**

```java
package com.studyos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "areas")
public class Area {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    private String description;

    @Column(length = 7)
    private String color;

    @Column(length = 50)
    private String icon;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false)
    private boolean archived;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
```

*Por quê:* `IDENTITY` deixa o **banco** gerar o id. O `id` não tem setter (ninguém troca o id de um registro). `@PrePersist` e `@PreUpdate` preenchem os timestamps de forma explícita, sem mágica de auditoria. `Instant` é um ponto na linha do tempo em UTC e casa com `timestamptz`. Nenhum construtor foi declarado: o Java cria o padrão sem argumentos, que o JPA exige.

- [ ] **Passo 5: Criar `backend/src/main/java/com/studyos/repository/AreaRepository.java`**

```java
package com.studyos.repository;

import com.studyos.entity.Area;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AreaRepository extends JpaRepository<Area, Long> {

    List<Area> findAllByOrderByPositionAscNameAsc();

    List<Area> findByArchivedFalseOrderByPositionAscNameAsc();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    @Query("select coalesce(max(a.position), 0) from Area a")
    int findMaxPosition();
}
```

*Por quê:* é só uma **interface**. O Spring Data lê o nome de cada método e gera o SQL: `findByArchivedFalseOrderByPositionAscNameAsc` vira `WHERE archived = false ORDER BY position, name`. O `@Query` usa JPQL (SQL sobre entidades, não tabelas) para o caso que o nome não expressa bem.

- [ ] **Passo 6: Rodar e ver passar**

```bash
.\mvnw.cmd test "-Dtest=AreaRepositoryTest"
```

Esperado: `Tests run: 7, Failures: 0`. Aparece um `WARN ... duplicate key value violates unique constraint "uq_areas_name_lower"` no log: é o teste do índice único funcionando.

- [ ] **Passo 7: Commit**

```bash
cd ..
git add backend
git commit -m "feat: adiciona entidade Area e AreaRepository com testes"
```

---

### Task 3: DTOs, mapper e service (regras de negócio)

**Conceitos:** `record`, compact constructor, Bean Validation (`@NotBlank`, `@Size`, `@Pattern`, `@Min`), mapper, injeção de dependência por construtor, `@Transactional`, *dirty checking* do Hibernate, Mockito.

**Arquivos:**
- Criar: `exception/ResourceNotFoundException.java`, `exception/ConflictException.java`, `dto/AreaRequest.java`, `dto/AreaResponse.java`, `mapper/AreaMapper.java`, `service/AreaService.java` (todos em `backend/src/main/java/com/studyos/`)
- Teste: `backend/src/test/java/com/studyos/service/AreaServiceTest.java`

**Interfaces:**
- Consome: `AreaRepository` e `Area` (Task 2).
- Produz: `AreaRequest(String name, String description, String color, String icon, Integer position)`; `AreaResponse(Long id, String name, String description, String color, String icon, int position, boolean archived)`; `AreaMapper` com `toResponse(Area)`, `toEntity(AreaRequest)`, `updateEntity(Area, AreaRequest)`; `AreaService` com `list(boolean)`, `get(Long)`, `create(AreaRequest)`, `update(Long, AreaRequest)`, `archive(Long)`, `unarchive(Long)`, todos devolvendo `AreaResponse` (a lista devolve `List<AreaResponse>`); `ResourceNotFoundException(String)` e `ConflictException(String)`.

- [ ] **Passo 1: Escrever o teste do service**

Crie `backend/src/test/java/com/studyos/service/AreaServiceTest.java`:

```java
package com.studyos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.studyos.dto.AreaRequest;
import com.studyos.dto.AreaResponse;
import com.studyos.entity.Area;
import com.studyos.exception.ConflictException;
import com.studyos.exception.ResourceNotFoundException;
import com.studyos.mapper.AreaMapper;
import com.studyos.repository.AreaRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AreaServiceTest {

    @Mock
    private AreaRepository repository;

    private AreaService service;

    @BeforeEach
    void setUp() {
        service = new AreaService(repository, new AreaMapper());
    }

    private AreaRequest request(String name, Integer position) {
        return new AreaRequest(name, null, null, null, position);
    }

    @Test
    void criaAreaNoFimDaListaQuandoNaoInformaPosicao() {
        when(repository.existsByNameIgnoreCase("Estudos")).thenReturn(false);
        when(repository.findMaxPosition()).thenReturn(9);
        when(repository.save(any(Area.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AreaResponse response = service.create(request("Estudos", null));

        assertThat(response.name()).isEqualTo("Estudos");
        assertThat(response.position()).isEqualTo(10);
        assertThat(response.archived()).isFalse();
    }

    @Test
    void criaAreaComPosicaoInformada() {
        when(repository.existsByNameIgnoreCase("Estudos")).thenReturn(false);
        when(repository.save(any(Area.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AreaResponse response = service.create(request("Estudos", 3));

        assertThat(response.position()).isEqualTo(3);
        verify(repository, never()).findMaxPosition();
    }

    @Test
    void criarComNomeDuplicadoLancaConflito() {
        when(repository.existsByNameIgnoreCase("CLOUD")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request("CLOUD", null)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("CLOUD");

        verify(repository, never()).save(any(Area.class));
    }

    @Test
    void requestRemoveEspacosDoNome() {
        assertThat(request("  Estudos  ", null).name()).isEqualTo("Estudos");
    }

    @Test
    void atualizaCamposDaArea() {
        Area existente = new Area();
        existente.setName("Antigo");
        when(repository.findById(1L)).thenReturn(Optional.of(existente));
        when(repository.existsByNameIgnoreCaseAndIdNot("Novo", 1L)).thenReturn(false);

        AreaResponse response = service.update(1L, new AreaRequest("Novo", "Descrição", "#112233", "book", 4));

        assertThat(response.name()).isEqualTo("Novo");
        assertThat(response.description()).isEqualTo("Descrição");
        assertThat(response.color()).isEqualTo("#112233");
        assertThat(response.icon()).isEqualTo("book");
        assertThat(response.position()).isEqualTo(4);
    }

    @Test
    void atualizarAreaInexistenteLancaNaoEncontrado() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99L, request("Qualquer", null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void atualizarParaNomeDeOutraAreaLancaConflito() {
        when(repository.findById(1L)).thenReturn(Optional.of(new Area()));
        when(repository.existsByNameIgnoreCaseAndIdNot("CLOUD", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.update(1L, request("CLOUD", null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void arquivaEDesarquivaArea() {
        Area area = new Area();
        when(repository.findById(1L)).thenReturn(Optional.of(area));

        assertThat(service.archive(1L).archived()).isTrue();
        assertThat(service.unarchive(1L).archived()).isFalse();
    }

    @Test
    void buscarAreaInexistenteLancaNaoEncontrado() {
        when(repository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(42L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("42");
    }
}
```

*Como ler:* aqui **não há Spring nem banco**. `@Mock` cria um repository de mentira. `when(...).thenReturn(...)` ensina o falso a responder, e `verify(..., never())` confere que algo **não** foi chamado. É o teste mais rápido do projeto: roda em milissegundos.

- [ ] **Passo 2: Rodar e ver falhar**

```bash
cd backend
.\mvnw.cmd test "-Dtest=AreaServiceTest"
```

Esperado: erro de compilação (faltam `AreaService`, `AreaMapper`, `AreaRequest`, as exceções).

- [ ] **Passo 3: Criar as exceções**

`backend/src/main/java/com/studyos/exception/ResourceNotFoundException.java`:

```java
package com.studyos.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
```

`backend/src/main/java/com/studyos/exception/ConflictException.java`:

```java
package com.studyos.exception;

public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
```

*Por quê:* o service não conhece HTTP. Ele só avisa "não encontrei" ou "isso conflita"; quem traduz para `404` e `409` é o handler da Task 4.

- [ ] **Passo 4: Criar os DTOs**

`backend/src/main/java/com/studyos/dto/AreaRequest.java`:

```java
package com.studyos.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AreaRequest(
        @NotBlank(message = "é obrigatório") @Size(max = 100, message = "deve ter no máximo 100 caracteres") String name,
        @Size(max = 1000, message = "deve ter no máximo 1000 caracteres") String description,
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "deve estar no formato #RRGGBB") String color,
        @Size(max = 50, message = "deve ter no máximo 50 caracteres") String icon,
        @Min(value = 0, message = "não pode ser negativa") Integer position) {

    public AreaRequest {
        if (name != null) {
            name = name.strip();
        }
    }
}
```

`backend/src/main/java/com/studyos/dto/AreaResponse.java`:

```java
package com.studyos.dto;

public record AreaResponse(
        Long id,
        String name,
        String description,
        String color,
        String icon,
        int position,
        boolean archived) {
}
```

*Por quê:* um `record` é uma classe imutável com construtor, getters (`name()`, sem prefixo `get`), `equals` e `toString` prontos. O **compact constructor** (o bloco sem parênteses) roda antes de guardar os campos e aqui tira espaços das pontas do nome: `"  Estudos "` vira `"Estudos"`, e a validação `@NotBlank` já enxerga o valor limpo. O `AreaResponse` não tem `createdAt`: a API só expõe o que a tela precisa. `position` é `Integer` (pode ser `null` = "coloque no fim") no pedido, e `int` na resposta (sempre existe).

- [ ] **Passo 5: Criar o mapper**

`backend/src/main/java/com/studyos/mapper/AreaMapper.java`:

```java
package com.studyos.mapper;

import com.studyos.dto.AreaRequest;
import com.studyos.dto.AreaResponse;
import com.studyos.entity.Area;
import org.springframework.stereotype.Component;

@Component
public class AreaMapper {

    public AreaResponse toResponse(Area area) {
        return new AreaResponse(
                area.getId(),
                area.getName(),
                area.getDescription(),
                area.getColor(),
                area.getIcon(),
                area.getPosition(),
                area.isArchived());
    }

    public Area toEntity(AreaRequest request) {
        Area area = new Area();
        updateEntity(area, request);
        return area;
    }

    public void updateEntity(Area area, AreaRequest request) {
        area.setName(request.name());
        area.setDescription(request.description());
        area.setColor(request.color());
        area.setIcon(request.icon());
        if (request.position() != null) {
            area.setPosition(request.position());
        }
    }
}
```

*Por quê escrever à mão:* você vê exatamente o que é copiado de onde, e entende o que ferramentas como MapStruct fariam por baixo. Na atualização, `position == null` significa "não mexa na posição".

- [ ] **Passo 6: Criar o service**

`backend/src/main/java/com/studyos/service/AreaService.java`:

```java
package com.studyos.service;

import com.studyos.dto.AreaRequest;
import com.studyos.dto.AreaResponse;
import com.studyos.entity.Area;
import com.studyos.exception.ConflictException;
import com.studyos.exception.ResourceNotFoundException;
import com.studyos.mapper.AreaMapper;
import com.studyos.repository.AreaRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AreaService {

    private final AreaRepository repository;
    private final AreaMapper mapper;

    public AreaService(AreaRepository repository, AreaMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<AreaResponse> list(boolean includeArchived) {
        List<Area> areas = includeArchived
                ? repository.findAllByOrderByPositionAscNameAsc()
                : repository.findByArchivedFalseOrderByPositionAscNameAsc();
        return areas.stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public AreaResponse get(Long id) {
        return mapper.toResponse(findOrThrow(id));
    }

    @Transactional
    public AreaResponse create(AreaRequest request) {
        if (repository.existsByNameIgnoreCase(request.name())) {
            throw new ConflictException("Já existe uma área com o nome '" + request.name() + "'");
        }
        Area area = mapper.toEntity(request);
        if (request.position() == null) {
            area.setPosition(repository.findMaxPosition() + 1);
        }
        return mapper.toResponse(repository.save(area));
    }

    @Transactional
    public AreaResponse update(Long id, AreaRequest request) {
        Area area = findOrThrow(id);
        if (repository.existsByNameIgnoreCaseAndIdNot(request.name(), id)) {
            throw new ConflictException("Já existe uma área com o nome '" + request.name() + "'");
        }
        mapper.updateEntity(area, request);
        return mapper.toResponse(area);
    }

    @Transactional
    public AreaResponse archive(Long id) {
        return setArchived(id, true);
    }

    @Transactional
    public AreaResponse unarchive(Long id) {
        return setArchived(id, false);
    }

    private AreaResponse setArchived(Long id, boolean archived) {
        Area area = findOrThrow(id);
        area.setArchived(archived);
        return mapper.toResponse(area);
    }

    private Area findOrThrow(Long id) {
        return repository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Área " + id + " não encontrada"));
    }
}
```

*Por quê:* **injeção por construtor** (`final` + construtor) deixa claro o que a classe precisa e permite criar o service nos testes com `new`. `@Transactional` abre uma transação por método: ou tudo é gravado, ou nada. Repare que `update` e `archive` **não chamam `save`**: o Hibernate vigia a entidade carregada (*dirty checking*) e, ao fechar a transação, grava as diferenças sozinho. O teste de integração da Task 5 prova que isso realmente persiste.

- [ ] **Passo 7: Rodar e ver passar**

```bash
.\mvnw.cmd test "-Dtest=AreaServiceTest"
```

Esperado: `Tests run: 9, Failures: 0`. Um aviso sobre "Mockito is currently self-attaching" pode aparecer: é sobre versões futuras do Java e pode ser ignorado por ora.

- [ ] **Passo 8: Commit**

```bash
cd ..
git add backend
git commit -m "feat: adiciona AreaService com DTOs, mapper e regras de negocio"
```

---

### Task 4: Controller e tratamento de erros

**Conceitos:** `@RestController`, `@RequestMapping`, `@Valid`, `ResponseEntity`, status HTTP (`201`, `404`, `409`), `ProblemDetail`, `@RestControllerAdvice`, `@WebMvcTest`, `MockMvc`.

**Arquivos:**
- Criar: `controller/AreaController.java`, `exception/GlobalExceptionHandler.java` (em `backend/src/main/java/com/studyos/`)
- Teste: `backend/src/test/java/com/studyos/controller/AreaControllerTest.java`

**Interfaces:**
- Consome: `AreaService`, `AreaRequest`, `AreaResponse`, `ResourceNotFoundException`, `ConflictException` (Task 3).
- Produz: endpoints `GET /api/areas?includeArchived=`, `GET /api/areas/{id}`, `POST /api/areas` (`201` + `Location`), `PUT /api/areas/{id}`, `PATCH /api/areas/{id}/archive`, `PATCH /api/areas/{id}/unarchive`. Erros: `400` (`title: "Validation failed"`, `errors: {campo: mensagem}`), `404`, `409` com `detail`.

- [ ] **Passo 1: Escrever o teste do controller**

Crie `backend/src/test/java/com/studyos/controller/AreaControllerTest.java`:

```java
package com.studyos.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.studyos.dto.AreaRequest;
import com.studyos.dto.AreaResponse;
import com.studyos.exception.ConflictException;
import com.studyos.exception.ResourceNotFoundException;
import com.studyos.service.AreaService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AreaController.class)
class AreaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AreaService service;

    private AreaResponse area(long id, String name) {
        return new AreaResponse(id, name, null, null, null, 1, false);
    }

    @Test
    void listaAreas() throws Exception {
        when(service.list(false)).thenReturn(List.of(area(1, "FACULDADE"), area(2, "CLOUD")));

        mockMvc.perform(get("/api/areas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("FACULDADE"));
    }

    @Test
    void listaAreasIncluindoArquivadas() throws Exception {
        when(service.list(true)).thenReturn(List.of(area(1, "FACULDADE")));

        mockMvc.perform(get("/api/areas").param("includeArchived", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void criaAreaDevolve201ComLocation() throws Exception {
        when(service.create(any(AreaRequest.class))).thenReturn(area(10, "Estudos"));

        mockMvc.perform(post("/api/areas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Estudos", "color": "#112233"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/areas/10"))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name").value("Estudos"));
    }

    @Test
    void nomeEmBrancoDevolve400ComErroPorCampo() throws Exception {
        mockMvc.perform(post("/api/areas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "   "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors.name").value("é obrigatório"));
    }

    @Test
    void corForaDoFormatoDevolve400() throws Exception {
        mockMvc.perform(post("/api/areas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Estudos", "color": "vermelho"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.color").value("deve estar no formato #RRGGBB"));
    }

    @Test
    void jsonMalFormadoDevolve400() throws Exception {
        mockMvc.perform(post("/api/areas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ isto não é json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nomeDuplicadoDevolve409() throws Exception {
        when(service.create(any(AreaRequest.class))).thenThrow(new ConflictException("Já existe uma área com o nome 'CLOUD'"));

        mockMvc.perform(post("/api/areas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "CLOUD"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Já existe uma área com o nome 'CLOUD'"));
    }

    @Test
    void areaInexistenteDevolve404() throws Exception {
        when(service.get(99L)).thenThrow(new ResourceNotFoundException("Área 99 não encontrada"));

        mockMvc.perform(get("/api/areas/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Área 99 não encontrada"));
    }

    @Test
    void idNaoNumericoDevolve400() throws Exception {
        mockMvc.perform(get("/api/areas/abc")).andExpect(status().isBadRequest());
    }

    @Test
    void arquivaArea() throws Exception {
        when(service.archive(eq(1L))).thenReturn(new AreaResponse(1L, "CLOUD", null, null, null, 6, true));

        mockMvc.perform(patch("/api/areas/1/archive"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archived").value(true));
    }
}
```

*Como ler:* `@WebMvcTest(AreaController.class)` sobe **só** a camada web (controller, validação, handlers de erro), sem banco e sem o service real. `@MockitoBean` coloca um `AreaService` falso no lugar. O `MockMvc` simula requisições HTTP sem abrir porta. Os blocos `"""..."""` são *text blocks* do Java, ótimos para JSON.

- [ ] **Passo 2: Rodar e ver falhar**

```bash
cd backend
.\mvnw.cmd test "-Dtest=AreaControllerTest"
```

Esperado: erro de compilação (falta `AreaController`).

- [ ] **Passo 3: Criar `backend/src/main/java/com/studyos/controller/AreaController.java`**

```java
package com.studyos.controller;

import com.studyos.dto.AreaRequest;
import com.studyos.dto.AreaResponse;
import com.studyos.service.AreaService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/areas")
public class AreaController {

    private final AreaService service;

    public AreaController(AreaService service) {
        this.service = service;
    }

    @GetMapping
    public List<AreaResponse> list(@RequestParam(defaultValue = "false") boolean includeArchived) {
        return service.list(includeArchived);
    }

    @GetMapping("/{id}")
    public AreaResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    public ResponseEntity<AreaResponse> create(@Valid @RequestBody AreaRequest request) {
        AreaResponse created = service.create(request);
        return ResponseEntity.created(URI.create("/api/areas/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public AreaResponse update(@PathVariable Long id, @Valid @RequestBody AreaRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/archive")
    public AreaResponse archive(@PathVariable Long id) {
        return service.archive(id);
    }

    @PatchMapping("/{id}/unarchive")
    public AreaResponse unarchive(@PathVariable Long id) {
        return service.unarchive(id);
    }
}
```

*Por quê:* o controller só **traduz**: lê o HTTP, delega ao service e devolve a resposta. Nenhuma regra de negócio. `@Valid` dispara as anotações do `AreaRequest`. Criar devolve `201 Created` com o cabeçalho `Location` apontando para o novo recurso, que é a convenção REST. `@RestController` já converte retornos para JSON.

- [ ] **Passo 4: Criar `backend/src/main/java/com/studyos/exception/GlobalExceptionHandler.java`**

```java
package com.studyos.exception;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Resource not found");
        return problem;
    }

    @ExceptionHandler(ConflictException.class)
    public ProblemDetail handleConflict(ConflictException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Conflict");
        return problem;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult()
                .getFieldErrors()
                .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));

        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Um ou mais campos são inválidos");
        problem.setTitle("Validation failed");
        problem.setProperty("errors", errors);
        return handleExceptionInternal(ex, problem, headers, status, request);
    }
}
```

*Por quê:* `@RestControllerAdvice` é um "pega-erros" global: qualquer exceção lançada em qualquer controller passa por aqui, e **todos** os erros da API saem no mesmo formato (`ProblemDetail`, padrão RFC 9457). Estender `ResponseEntityExceptionHandler` faz o Spring tratar sozinho os erros comuns (JSON malformado, `id` não numérico, método não suportado) como `400`/`405`/`415`, em vez de `500`. Só sobrescrevemos a validação para acrescentar o mapa `errors` com **um erro por campo**, que o Angular usará para mostrar a mensagem embaixo de cada input.

- [ ] **Passo 5: Rodar e ver passar**

```bash
.\mvnw.cmd test "-Dtest=AreaControllerTest"
```

Esperado: `Tests run: 10, Failures: 0`.

- [ ] **Passo 6: Commit**

```bash
cd ..
git add backend
git commit -m "feat: adiciona AreaController e tratamento global de erros"
```

---

### Task 5: Teste ponta a ponta, teste manual e Pull Request

**Conceitos:** `@SpringBootTest` (a aplicação inteira), pirâmide de testes, cliente HTTP (Bruno), Pull Request com *squash*.

**Arquivos:**
- Teste: `backend/src/test/java/com/studyos/AreaApiIntegrationTest.java`

**Interfaces:**
- Consome: tudo das Tasks 1 a 4.

- [ ] **Passo 1: Criar o teste de integração**

`backend/src/test/java/com/studyos/AreaApiIntegrationTest.java`:

```java
package com.studyos;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AreaApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void fluxoCompletoDeUmaArea() throws Exception {
        mockMvc.perform(get("/api/areas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("FACULDADE"));

        mockMvc.perform(post("/api/areas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Idiomas Extras", "color": "#AABBCC"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.position").value(10));

        mockMvc.perform(post("/api/areas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "idiomas extras"}
                                """))
                .andExpect(status().isConflict());

        mockMvc.perform(put("/api/areas/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Idiomas", "description": "Espanhol e francês"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Idiomas"));

        mockMvc.perform(put("/api/areas/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Idiomas", "description": "Espanhol, francês e italiano"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.position").value(10));

        mockMvc.perform(get("/api/areas/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Idiomas"))
                .andExpect(jsonPath("$.description").value("Espanhol, francês e italiano"));

        mockMvc.perform(patch("/api/areas/10/archive"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archived").value(true));

        mockMvc.perform(get("/api/areas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(9));

        mockMvc.perform(get("/api/areas").param("includeArchived", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(10));

        mockMvc.perform(get("/api/areas/9999")).andExpect(status().isNotFound());
    }
}
```

*Por quê:* os testes anteriores usam falsos (service falso, controller sem banco). Este usa **tudo de verdade**: HTTP → controller → service → repository → PostgreSQL. É o único que prova, por exemplo, que o *dirty checking* do `update` realmente grava, e que o `409` por nome com outra caixa funciona de ponta a ponta. Um teste só, caro e valioso: poucos testes assim, muitos dos rápidos.

- [ ] **Passo 2: Rodar a suíte completa**

```bash
cd backend
.\mvnw.cmd test
```

Esperado: `Tests run: 28, Failures: 0, Errors: 0` e `BUILD SUCCESS` (1 `StudyosApplicationTests` + 7 repository + 9 service + 10 controller + 1 integração). Demora cerca de 1 minuto, porque cada tipo de teste sobe seu próprio contexto.

- [ ] **Passo 3: Teste manual com um cliente HTTP (Bruno)**

Os testes automáticos garantem o que previmos; o teste manual mostra a API como o Angular a verá.

1. Baixe e instale o **Bruno** pelo site oficial (usebruno.com).
2. Crie uma coleção chamada `StudyOS` na pasta `C:\Projetos\studyos\backend\api` (as requisições viram arquivos de texto versionáveis).
3. Rode o backend (▶ em `StudyosApplication`; Docker Compose no ar) e crie estas requisições:

| Nome | Método | URL | Corpo (JSON) | Esperado |
|---|---|---|---|---|
| Listar áreas | GET | `http://localhost:8080/api/areas` | — | `200`, 9 áreas |
| Criar área | POST | `http://localhost:8080/api/areas` | `{"name": "Estudos Livres", "color": "#3B82F6"}` | `201`, `position` 10, cabeçalho `Location` |
| Criar duplicada | POST | idem | `{"name": "estudos livres"}` | `409` com `detail` |
| Criar inválida | POST | idem | `{"name": "  "}` | `400` com `errors.name` |
| Editar | PUT | `http://localhost:8080/api/areas/10` | `{"name": "Estudos Gerais"}` | `200` |
| Arquivar | PATCH | `http://localhost:8080/api/areas/10/archive` | — | `200`, `archived: true` |
| Listar (com arquivadas) | GET | `http://localhost:8080/api/areas?includeArchived=true` | — | `200`, 10 áreas |
| Não existe | GET | `http://localhost:8080/api/areas/9999` | — | `404` |

(Para o POST e o PUT, escolha o corpo **JSON**.) Se algum resultado for diferente do esperado, anote o que apareceu.

- [ ] **Passo 4: Commit, push e Pull Request**

```bash
cd ..
git add backend
git commit -m "test: adiciona teste de integracao do fluxo completo de areas"
git push -u origin feat/areas-backend
```

No GitHub, abra o PR (`base: main` ← `compare: feat/areas-backend`), título `Fatia 1A: backend de áreas`, revise o diff e, desta vez, use **Squash and merge** para ver a diferença em relação ao merge anterior.

- [ ] **Passo 5: Sincronizar a máquina**

```bash
git switch main
git pull
git branch -d feat/areas-backend
git fetch --prune
```

Como o merge foi *squash*, o Git não reconhece a branch como mergeada e o `-d` pode recusar. Nesse caso (e só depois de confirmar que o PR foi mergeado) use `git branch -D feat/areas-backend`.

- [ ] **Passo 6: Checklist de conclusão da Fatia 1A**

- [ ] `.\mvnw.cmd test` → 28 testes, BUILD SUCCESS
- [ ] Bruno: as 8 requisições com os status esperados
- [ ] Banco: `select count(*) from areas;` mostra 10 (9 + a área de teste)
- [ ] PR mergeado e branch removida
