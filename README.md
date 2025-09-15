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
│  │  │  ├─ dev/kotlinbr/app
│  │  │  │  ├─ config/Config.kt   # Carrega AppConfig (env, server, db)
│  │  │  │  └─ http/HTTP.kt       # Middleware HTTP: compressão, logs, X-Request-ID
│  │  │  ├─ dev/kotlinbr/domain   # Entidades de domínio (ex.: Link)
│  │  │  ├─ dev/kotlinbr/infrastructure
│  │  │  │  ├─ db/DatabaseFactory.kt  # Hikari + Flyway + health check
│  │  │  │  └─ db/tables          # Tabelas Exposed (LinksTable etc.)
│  │  │  └─ dev/kotlinbr/interfaces/http
│  │  │     ├─ Routing.kt         # Rotas HTTP (/health, /health/ready, /env, /)
│  │  │     └─ Serialization.kt   # ContentNegotiation + JSON
│  │  └─ resources
│  │     ├─ application.conf      # Configurações baseadas em env (dev/prod/test)
│  │     ├─ application.yaml      # Suporte YAML (opcional)
│  │     ├─ db/migration          # Migrações Flyway (V1__create_links.sql)
│  │     └─ logback.xml           # Configuração de logs
│  └─ test
│     └─ kotlin                   # Testes unitários e de integração
└─ README.md
```


## Como rodar/desenvolver localmente (Gradle)

**Pré-requisitos:**
- Java 21 (JDK) instalado
- Docker (para subir dependências como Postgres/Redis)

**Comandos úteis:**
- Rodar testes: `./gradlew test`
- Build completo: `./gradlew build` (inclui `ktlintCheck` e `detekt`)
- Executar o servidor: `./gradlew run`

**Dependências via Docker (Postgres e Redis):**
- Subir dependências: `./gradlew dockerDepsUp`
- Parar containers (mantém dados): `./gradlew dockerDepsStop`
- Remover containers (mantém dados): `./gradlew dockerDepsDown`
- Recriar deps: `./gradlew dockerDepsRecreate`
- Atualizar imagens: `./gradlew dockerDepsPull`
- Resetar banco (apaga volume): `./gradlew dockerDbReset`

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


## Contribuição


- **Issues e PRs:** Prefira mensagens e commits em inglês (código), mas descrições podem ser em pt-BR. Se mensagens de commit em inglês estiverem gerando atrito relevante, fique à vontade para escrevê-las em pt-BR.
- **Padrões desejáveis:** pequenas PRs, descrição clara, testes cobrindo alterações, logs/erros amigáveis, atenção à observabilidade e ao desempenho.


## Licença

Este projeto utiliza a licença MIT. Veja o arquivo LICENSE para detalhes.

