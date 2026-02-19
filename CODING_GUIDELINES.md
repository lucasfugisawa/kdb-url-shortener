# Coding Guidelines

Este documento define os padrões de desenvolvimento e boas práticas para o projeto **kdb-url-shortener**. O objetivo é manter a consistência do código, facilitar a manutenção e garantir a qualidade do software.

## 1. Idioma e Nomenclatura

- **Código e Documentação Técnica**: Todo o código (nomes de classes, variáveis, funções), logs, mensagens de erro da API e KDoc devem ser escritos em **en-US**.
- **Commits**: Mensagens de commit devem ser preferencialmente em **en-US**.
- **Pull Requests**: Títulos e descrições podem ser em **en-US** ou **pt-BR**.

## 2. Estilo de Código (Kotlin)

- **Wildcard Imports**: São estritamente proibidos. Importe cada classe ou função explicitamente.
- **Formatação**: O projeto utiliza o `ktlint`. Execute `./gradlew ktlintFormat` antes de enviar seu código.
- **Análise Estática**: Utilizamos o `detekt`. Garanta que seu código não possui violações executando `./gradlew detekt`.
- **Naming**: Siga as convenções oficiais do Kotlin (PascalCase para classes, camelCase para variáveis/funções).
- **Tratamento de Datas**: Use sempre `java.time.OffsetDateTime` para garantir a consistência de fuso horário.

## 3. Arquitetura e Padrões

- **Injeção de Dependência**: Atualmente o projeto não utiliza um framework de DI (como Koin ou Dagger). No entanto, evite instanciar repositórios ou serviços repetidamente dentro de loops ou múltiplos locais na mesma classe/rota. Instancie-os uma vez no nível apropriado (ex: no início da configuração da rota).
- **DTOs (Data Transfer Objects)**: Toda comunicação externa (API) deve usar DTOs definidos em `interfaces/http/dto`. Não exponha entidades de domínio diretamente.
- **Imutabilidade**: Prefira `val` a `var` sempre que possível. Use `data class` para representar estruturas de dados.

## 4. Testes

- **Unitários**: Devem ser rápidos e não depender de recursos externos.
- **Integração**: Devem ser anotados com `@Tag("integration")`. O projeto utiliza **Testcontainers** para fornecer um banco de dados PostgreSQL real durante os testes de integração.
- **Cobertura**: Novas funcionalidades ou correções de bugs devem vir acompanhadas de testes que validem tanto o "caminho feliz" quanto casos de erro.

## 5. Observabilidade

- **Logs**: Use SLF4J com Logback. Registre eventos importantes, mas evite logs excessivos ou sensíveis em produção.
- **Error Handling**: Use o mecanismo de `StatusPages` no Ktor para centralizar o tratamento de exceções e retornar respostas consistentes.

## 6. Git Workflow

- **Hooks**: Recomenda-se instalar o git hook de pre-push: `./gradlew installGitHookPrePush`. Isso garante que testes e checagens de estilo rodem localmente antes de você enviar o código.
- **Tamanho das PRs**: Prefira Pull Requests pequenas e focadas em uma única tarefa ou funcionalidade.
