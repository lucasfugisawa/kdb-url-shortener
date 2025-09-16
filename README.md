# kdb-url-shortener

Encurtador de URLs escrito em Kotlin/Ktor. 

Este repositório será utilizado como projeto de aprendizado colaborativo na comunidade Kotlin Devs Brasil (KDB). 

A iniciativa visa ajudar pessoas desenvolvedoras iniciantes (ou em transição) a praticar backend com Kotlin em um cenário próximo ao de uma empresa real, desenvolvendo habilidades técnicas (Kotlin, Ktor, bancos de dados, testes, Docker, boas práticas) e comportamentais (comunicação, colaboração, revisão de código, gestão de tarefas), além de adquirir experiência prática e portfólio.


## Visão geral

- Stack principal: Kotlin (JVM 21), Ktor (Netty), Exposed (SQL), PostgreSQL, Flyway (migrações), Redis (futuro uso), Logback (logs).
- Endpoints iniciais:
  - `GET /` → retorno simples "Hello World!" (placeholder).
  - `GET /health` → `{ "status": "ok" }` para verificação básica.
  - `GET /health/ready` → checa conectividade com o banco; retorna 200 quando a aplicação está pronta para receber tráfego.
  - `GET /env` → expõe o ambiente atual (dev, prod, test).
- Observabilidade simples: cabeçalho `X-Request-ID`, logs estruturados com informações de requisição e latência.


## O que é e como funciona um URL Shortener

Um **URL Shortener** (encurtador de links) é um serviço que transforma um endereço longo, difícil de compartilhar, em um link curto e simples. Exemplo:
- Longo: https://www.exemplo.com/artigos/ktor-introducao?utm_source=newsletter&utm_medium=email
- Curto: https://sho.rt/abc123

**Por que usar:**
- Facilita o compartilhamento em redes sociais, mensagens e materiais impressos.
- Melhora a estética dos links e reduz erros de digitação.
- Possibilita coleta de métricas (cliques, origem, dispositivo) e aplicação de regras como expiração do link.

**Como funciona (alto nível):**
1) Criação do link curto
   - O cliente envia a URL original para o serviço.
   - A aplicação gera um código curto (ex.: "abc123") ou usa um alias personalizado (ex.: "kdb").
   - O par código → URL original é salvo no banco de dados.
   - A API retorna a URL curta completa (ex.: https://sho.rt/abc123).
2) Redirecionamento
   - Quando alguém acessa https://sho.rt/abc123, o servidor procura o código no banco e responde com um redirecionamento HTTP (geralmente 302) para a URL original.
3) Métricas e regras (opcionais)
   - Cada clique pode ser registrado para gerar relatórios.
   - É possível definir expiração (TTL), limites de uso, proteção por senha, entre outras políticas.

**Exemplo prático (ilustrativo):**
- **Criar** um link curto:
  - `POST /links`
    - Body JSON: `{ "url": "https://kotlinlang.org/docs/home.html", "alias": "kotlin-docs" }`
    - Resposta: `{ "code": "kotlin-docs", "shortUrl": "https://sho.rt/kotlin-docs" }`
- **Acessar** o link curto:
  - `GET /kotlin-docs` → `302 Location: https://kotlinlang.org/docs/home.html`


## Estrutura do projeto

```
.
├─ Dockerfile                      # Build multi-stage e imagem de runtime (JRE 21)
├─ docker-compose.yml              # Postgres, Redis e App (perfis: deps/app)
├─ build.gradle.kts                # Dependências, plugins e tasks auxiliares
├─ settings.gradle.kts             # Nome do projeto
├─ detekt.yml                      # Regras de análise estática
├─ gradle*                         # Wrapper do Gradle
├─ src
│  ├─ main
│  │  ├─ kotlin
│  │  │  ├─ Application.kt        # Ponto de entrada (EngineMain) + module()
│  │  │  └─ dev/kotlinbr/utlshortener
│  │  │     ├─ app
│  │  │     │  ├─ config/Config.kt     # Carrega AppConfig (env, server, db)
│  │  │     │  └─ http/HTTP.kt         # Middleware HTTP: compressão, logs, X-Request-ID
│  │  │     ├─ domain
│  │  │     │  └─ Link.kt              # Entidade de domínio
│  │  │     ├─ infrastructure
│  │  │     │  ├─ db/DatabaseFactory.kt    # Hikari + Flyway + health check
│  │  │     │  └─ db/tables
│  │  │     │     ├─ HelloTable.kt         # Tabela de exemplo
│  │  │     │     └─ LinksTable.kt         # Tabela de links
│  │  │     │  
│  │  │     │  └─ repository
│  │  │     │     ├─ HelloRepository.kt    # Repositório de exemplo
│  │  │     │     └─ LinksRepository.kt    # Repositório de links
│  │  │     └─ interfaces/http
│  │  │        ├─ Routing.kt               # Rotas HTTP (/health, /health/ready, /env, /)
│  │  │        ├─ Serialization.kt         # ContentNegotiation + JSON
│  │  │        └─ dto/LinkResponse.kt      # DTO de resposta para links
│  │  └─ resources
│  │     ├─ application.conf               # Configurações baseadas em env (dev/prod/test)
│  │     ├─ application.yaml               # Suporte YAML (opcional)
│  │     ├─ db/migration                   # Migrações Flyway (V1__create_links.sql)
│  │     └─ logback.xml                    # Configuração de logs
│  └─ test
│     └─ kotlin                            # Testes unitários e de integração
│        ├─ ApiLinksIT.kt                  # Teste de integração dos endpoints de links
│        ├─ ApplicationTest.kt             # Testes do módulo principal
│        ├─ ErrorFormatTest.kt             # Teste de formato de erros
│        └─ LinksMigrationIT.kt            # Teste de migrações Flyway
└─ README.md
```


## Como rodar/desenvolver localmente (Gradle)

### Pré-requisitos
- Java 21 (JDK) instalado
- Docker (para subir dependências como Postgres/Redis)

Nota para Windows:
- Recomenda-se usar o Windows PowerShell nas instruções abaixo, pois ele suporta invocar o wrapper do Gradle como `./gradlew`.
- Alternativamente, no CMD use `gradlew.bat` (sem `./`). No Git Bash também é possível usar `./gradlew`.

### Comandos úteis
- Rodar testes: `./gradlew test`
- Build completo: `./gradlew build` (inclui `ktlintCheck` e `detekt`)
- Executar o servidor: `./gradlew run`

### Dependências via Docker (Postgres e Redis)
- Subir dependências: `./gradlew dockerDepsUp`
- Parar containers (mantém dados): `./gradlew dockerDepsStop`
- Remover containers (mantém dados): `./gradlew dockerDepsDown`
- Recriar deps: `./gradlew dockerDepsRecreate`
- Atualizar imagens: `./gradlew dockerDepsPull`
- Resetar banco (apaga volume): `./gradlew dockerDbReset`

### Ordem e pré-requisitos (importante)
- **Pré-requisito:** Docker Desktop/Engine em execução e com Docker Compose v2 disponível (comando `docker compose`).
- **Primeira vez (ou após longo tempo):** rode `dockerDepsPull` para baixar as imagens e depois `dockerDepsUp`.
- **Ciclo típico de desenvolvimento:**
  1) `dockerDepsUp` — cria/sobe Postgres e Redis em background (perfil deps). Se já existirem, apenas inicia.
  2) Desenvolva e rode a aplicação: `./gradlew run` (a aplicação aponta por padrão para o Postgres em localhost).
  3) `dockerDepsStop` — pausa os containers, mantendo os dados no volume.
- **Quando algo "quebrar" nos containers sem alterar dados:** use `dockerDepsRecreate` para forçar a recriação dos containers (mantém volumes e dados).
- **Para limpar containers e rede (mantendo os volumes):** use `dockerDepsDown`.
- **Para reset total do banco (apagando volume do Postgres):** use `dockerDbReset`. Atenção: isso apaga todos os dados.

Observações e dicas:
- Healthcheck: o Postgres tem healthcheck no docker-compose. Após `dockerDepsUp`, aguarde alguns segundos até o serviço ficar saudável. Você pode checar com `docker compose -f docker-compose.yml ps`.
- Perfis: usamos o perfil `deps` no Compose para subir apenas Postgres e Redis. Os comandos Gradle já passam `--profile deps` quando necessário.
- Sobre migrações: por padrão, ao iniciar a aplicação local (Gradle run), as migrações do Flyway são executadas automaticamente para garantir que a tabela `links` exista.
- Parar tudo manualmente: se você tiver subido a stack completa via Compose (incluindo `app`), `dockerDepsDown` também derruba os serviços do perfil atual do projeto. Para um reset completo com remoção de volume, prefira `dockerDbReset`. 

**Configuração padrão (dev):**
- Ambiente: `APP_ENV=dev` (padrão)
- Banco (localhost): `jdbc:postgresql://localhost:5432/kdb_url_shortener` com user `kdb_url_shortener` e senha `kdb-url-shortener-pwd`
- Para sobrescrever via ambiente: `DB_URL`, `DB_USER`, `DB_PASSWORD`
- Para pular o banco temporariamente (ex.: demos rápidas): definir propriedade do sistema `-DAPP_SKIP_DB=true` (não recomendável para desenvolvimento real)

**Health checks locais:**
- `GET http://localhost:8080/health` → `{ "status": "ok" }`
- `GET http://localhost:8080/health/ready` → 200 quando a app conectou no banco


## Rodando tudo com Docker (stack semelhante ao prod)

**Pré-requisitos:** Docker e Docker Compose.

**Subir a stack completa (Postgres + Redis + App):**
- `docker compose up --build`

**Serviços:**
- postgres (imagem: `postgres:16`)
- redis (imagem: `redis:7`)
- app (build a partir do Dockerfile usando Eclipse Temurin 21)

**Variáveis usadas pelo container da aplicação (definidas no compose):**
- `APP_ENV=prod`
- `APP_RUN_MIGRATIONS=true` (roda migrações Flyway no startup)
- `DB_URL=jdbc:postgresql://postgres:5432/kdb_url_shortener`
- `DB_USER=kdb_url_shortener`
- `DB_PASSWORD=kdb-url-shortener-pwd`

**Verificando saúde:**
- `GET http://localhost:8080/health` → 200 `{ "status": "ok" }`
- `GET http://localhost:8080/health/ready` → 200 quando o Postgres estiver pronto e a app conectada

**Parar/remover:**
- Ctrl+C para parar; depois `docker compose down` para remover containers. Dados do Postgres ficam no volume `pgdata`.


## Qualidade de código

- Checagens: `./gradlew ktlintCheck detekt`
- Formatação automática: `./gradlew ktlintFormat`

## Hook do Git: pre-push (checagens antes de enviar código)

Por que instalar:
- Evita enviar código que quebra o build/testes/análises estáticas. O hook executa a tarefa `check`, que já inclui `ktlintCheck` e `detekt` configurados no projeto.

Como instalar o hook neste repositório:
- Execute: `./gradlew installGitHookPrePush`
  - Isso cria/atualiza o arquivo `.git/hooks/pre-push` com um script que roda `./gradlew check` antes do push.
  - Se a checagem falhar, o push é abortado (você verá uma mensagem explicando o motivo).

Observações:
- O hook é instalado localmente (não vai para o repositório remoto). Cada colaborador precisa instalá-lo uma vez.
- Caso o diretório `.git` não exista (por exemplo, se você baixou um zip), o task avisará e não fará nada.
- Para remover, apague o arquivo `.git/hooks/pre-push`.
- No Windows, o Git para Windows executa hooks como scripts sh. O comando `./gradlew` funciona no PowerShell e no Git Bash; no CMD use `gradlew.bat`.

## Testes: como rodar e como funcionam

Este projeto separa testes unitários (rápidos) de testes de integração (mais lentos) que usam Testcontainers (PostgreSQL).

### Resumo de comandos
- Unit tests (padrão): `./gradlew test`
  - Executa JUnit 5 com `@Tag("integration")` excluído.
- Integration tests apenas: `./gradlew integrationTest`
  - Executa somente testes anotados com `@Tag("integration")`.
- Todos os testes (recomendado antes de push): `./gradlew check`
  - Executa unit (test) e integration (integrationTest), além das checagens estáticas (ktlint, detekt).

### Convenção de tags
- Qualquer teste que necessite de recursos externos (ex.: Docker/Testcontainers) deve ser anotado com `@Tag("integration")`.
- Testes puramente de JVM/unidade não são tagueados e rodam por padrão no `test`.

### Como o ambiente é controlado nos testes
- Os testes evitam depender de variáveis de ambiente reais do SO. Em vez disso, usam propriedades do sistema (JVM) para emular as variáveis esperadas pelo loader de configuração da app:
  - `APP_ENV`, `APP_SKIP_DB`, `APP_RUN_MIGRATIONS`
  - `DB_URL`, `DB_USER`, `DB_PASSWORD`, `DB_DRIVER`, `DB_POOL_MAX`
- Exemplos aparecem ao longo dos testes, por exemplo: `System.setProperty("APP_SKIP_DB", "true")` para pular o DB no bootstrap, ou a `BaseIntegrationTest` que injeta `DB_URL/USER/PASSWORD` a partir do container em execução.
- O carregador de config (`loadAppConfig`) lê primeiro do `ApplicationConfig` (overrides em memória via `testApplication`) e depois de propriedades do sistema/variáveis reais, permitindo testes determinísticos.

### Infra de testes
- `src/test/kotlin/dev/kotlinbr/utlshortener/testutils`
  - `BaseIntegrationTest.kt`: classe base JUnit que inicia um container PostgreSQL (Testcontainers) reutilizável e configura propriedades do sistema para a app (`APP_ENV=test`, `APP_RUN_MIGRATIONS=true`, credenciais `DB_*`). Marca testes com `@Tag("integration")`.
  - `TestDataFactory.kt`: builders e helpers com Exposed para inserir/selecionar links.
  - `TestClockUtils.kt`: utilitários de relógio fixo para timestamps determinísticos.
- Testes unitários ficam em `src/test/kotlin/dev/kotlinbr` e espelham a estrutura de pacotes do main.

### Testcontainers em CI
- Requisitos: um daemon Docker deve estar disponível ao runner. Modo privilegiado NÃO é obrigatório; Docker-in-Docker padrão ou socket do host são suficientes.
- Não é necessária config Gradle especial. Testes anotados com `@Tag("integration")` iniciarão containers PostgreSQL sob demanda.
- Reuse para velocidade (opcional, local): crie `~/.testcontainers.properties` com `testcontainers.reuse.enable=true` para permitir reuso entre execuções. Não faça commit desse arquivo.
- Se o CI limitar egress de rede, garanta que o Docker pode baixar `postgres:16-alpine`.

### Solução de problemas
- "Cannot connect to Docker": garanta que o Docker está instalado e que o usuário/runner atual tem acesso ao daemon.
- Testes de integração travam na primeira execução: o pull de imagens pode levar tempo em cache frio. Faça pre-pull das imagens ou use o helper `dockerDepsPull`.
- Para pular testes de integração temporariamente no CI, rode apenas `./gradlew test`. Para exigir ambos, use `./gradlew check`.

### Referências
- O filtro por tags do JUnit Platform está configurado no `build.gradle.kts`: a task `test` exclui `@Tag("integration")`, e a task `integrationTest` inclui somente essa tag.

## Contribuição

- **Issues e PRs:** Prefira mensagens e commits em inglês (código), mas descrições podem ser em pt-BR. Se mensagens de commit em inglês estiverem gerando atrito relevante, fique à vontade para escrevê-las em pt-BR.
- **Padrões desejáveis:** pequenas PRs, descrição clara, testes cobrindo alterações, logs/erros amigáveis, atenção à observabilidade e ao desempenho.


## Licença

Este projeto utiliza a licença MIT. Veja o arquivo LICENSE para detalhes.

