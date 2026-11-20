# Conserta Aí — API Java (ocorrências, triagem com IA e autenticação)

> **Global Solution 2026/2 — CitizenTech · FIAP 2TDS**
> Disciplina: **Java Advanced**

O **Conserta Aí** é um app em que o morador reporta problemas de zeladoria (buraco, poste apagado, lixo, árvore com risco de queda) e acompanha a resolução, enquanto o gestor da subprefeitura recebe uma **fila já classificada e priorizada**.

Esta API Java é o **núcleo do ecossistema**:
- é dona dos **usuários** e das **ocorrências**;
- **emite o JWT** que o app usa nesta API e na API .NET;
- faz a **triagem automática com IA generativa** (Spring AI);
- **consome do RabbitMQ** os apoios registrados na API .NET, para reordenar a fila do gestor.

| | |
|---|---|
| **Cidadão beneficiado** | Morador que vê o problema na rua e quer resolvê-lo sem ligar no 156 e ficar sem retorno |
| **Problema** | Reports mal descritos, duplicados e sem prioridade. O gestor gasta tempo classificando, e o cidadão não sabe o que acontece depois |
| **O que esta API entrega** | Triagem automática por IA (categoria, urgência, risco, órgão, resumo e detecção de duplicadas), fila priorizada e linha do tempo de status visível ao cidadão |
| **ODS** | **11** (cidades sustentáveis), **9** (infraestrutura) e **16** (instituições eficazes e transparentes) |

---

## Links da entrega

| | |
|---|---|
| **Deploy (Render)** | https://gs-java-5k10.onrender.com |
| **Swagger** | https://gs-java-5k10.onrender.com/swagger-ui.html |
| **Vídeo pitch** | https://youtu.be/oeh9sTXGQek |
| **API .NET do grupo** | https://consertaai-api-rm563454.azurewebsites.net/swagger/index.html |

> No plano gratuito do Render, o serviço hiberna sem uso. **A primeira chamada pode levar cerca de 1 minuto.**

## Sumário

1. [Arquitetura](#1-arquitetura)
2. [Requisitos da disciplina → implementação](#2-requisitos-da-disciplina--implementação)
3. [Como executar](#3-como-executar)
4. [Usuários de teste](#4-usuários-de-teste)
5. [Endpoints principais](#5-endpoints-principais)
6. [Autenticação JWT](#6-autenticação-jwt)
7. [Funcionalidade com Spring AI: triagem automática](#7-funcionalidade-com-spring-ai-triagem-automática)
8. [Arquitetura distribuída: opção A (RabbitMQ)](#8-arquitetura-distribuída-opção-a-rabbitmq)
9. [Integração com o app Mobile](#9-integração-com-o-app-mobile)
10. [Testes](#10-testes)
11. [Deploy no Render](#11-deploy-no-render)
12. [Integrantes](#12-integrantes)
13. [Declaração de uso de IA](#13-declaração-de-uso-de-ia)

---

## 1. Arquitetura

```mermaid
flowchart LR
    APP["📱 App Mobile (Expo)"]

    subgraph JAVA["API Java — Spring Boot (este repositório)"]
        AUTH["/auth/login<br/>emite JWT HS256"]
        OC["/ocorrencias<br/>CRUD + fila + histórico"]
        PUB["EventosPublisher<br/>(after commit, @Async)"]
        TRI["TriagemConsumer<br/>Spring AI"]
        APC["ApoiosConsumer"]
    end

    NET["API .NET<br/>apoios e comentários"]
    MQ{{"RabbitMQ<br/>exchange topic consertaai.eventos"}}
    PG[("PostgreSQL<br/>Flyway")]
    LLM["LLM<br/>Groq · ChatGPT · DeepSeek"]

    APP -- "login" --> AUTH
    APP -- "Bearer JWT" --> OC
    APP -- "mesmo Bearer JWT" --> NET
    OC --> PG
    OC -- "ocorrencia.criada" --> PUB --> MQ
    MQ -- "ocorrencia.criada" --> TRI
    TRI -- "classifica" --> LLM
    TRI --> PG
    NET -- "apoio.registrado / apoio.removido" --> MQ
    MQ -- "apoio.*" --> APC --> PG
    NET -. "GET /ocorrencias/{id}" .-> OC
```

```
br.com.fiap.consertaai
├── config/        SecurityConfig (JWT, papéis, CORS), OpenApiConfig
├── controller/    Auth, Categoria, Ocorrencia, Internal (chamado pela .NET)
├── domain/        entidades JPA e enums (StatusOcorrencia guarda as transições)
├── dto/           records de entrada/saída com Bean Validation
├── exception/     GlobalExceptionHandler (@RestControllerAdvice) + exceções de domínio
├── ia/            Spring AI: TriagemIaClient, TriagemRegras (fallback), TriagemService
├── messaging/     RabbitMQ: topologia, publicador, consumidores, eventos
├── repository/    Spring Data JPA (EntityGraph, Specification, paginação)
├── security/      TokenService (emite JWT), UsuarioLogado, handlers 401/403 em JSON
└── service/       regras de negócio e PrioridadeCalculator
```

### Modelo de dados

```
usuarios (1) ──< ocorrencias >── (1) categorias
                    │  └── possivel_duplicada_de ──> ocorrencias (auto-relacionamento)
                    └──< historico_status >── usuarios (responsável)
```

| Entidade | Papel no domínio |
|---|---|
| `Usuario` | Cidadão ou gestor. A senha é guardada como hash BCrypt |
| `Categoria` | Tipo de problema, órgão responsável e prazo de atendimento (SLA) |
| `Ocorrencia` | O problema reportado, com o resultado da triagem, a quantidade de apoios e a prioridade |
| `HistoricoStatus` | Cada mudança de status, com quem mudou e por quê. O cidadão acompanha por aqui |

O schema é versionado com **Flyway** (`src/main/resources/db/migration`). O Hibernate roda com `ddl-auto: validate`: ele confere se as entidades batem com o banco, mas nunca altera o banco.

---

## 2. Requisitos da disciplina → implementação

| # | Requisito (pontos) | Como foi atendido | Onde ver |
|---|---|---|---|
| 1 | **API REST do domínio** (20) | <ul><li>4 entidades relacionadas.</li><li>CRUD completo de `Ocorrencia` e `Categoria` com Spring Data JPA.</li><li>Paginação e ordenação via `Pageable`, com filtros por `Specification`.</li><li>`@EntityGraph` para evitar N+1.</li></ul> | `controller/`, `repository/`, `service/` |
| 2 | **Validação e erros** (10) | <ul><li>Bean Validation em todos os DTOs (`@NotBlank`, `@Size`, `@Email`, `@DecimalMin`...).</li><li>`@RestControllerAdvice` com o mesmo `ErroResponse` para 400, 401, 403, 404, 409, 422 e 500.</li><li>Os 401 e 403 do filtro de segurança saem no mesmo formato.</li></ul> | `exception/GlobalExceptionHandler.java`, `security/JsonSecurityHandlers.java` |
| 3 | **Spring Security + JWT** (20) | <ul><li>Login com BCrypt emite um JWT HS256.</li><li>Validação de assinatura, emissor, audiência e expiração.</li><li>Papéis `CIDADAO` e `GESTOR` protegendo endpoints por rota.</li><li>Regras de dono (só o autor edita).</li><li>Token compartilhado com a API .NET.</li></ul> | `config/SecurityConfig.java`, `security/TokenService.java` |
| 4 | **Spring AI** (20) | **Triagem automática** dentro do fluxo de abertura da ocorrência. O modelo devolve, em JSON estruturado:<ul><li>categoria;</li><li>urgência;</li><li>risco imediato;</li><li>órgão responsável;</li><li>resumo;</li><li>possível duplicada.</li></ul>Esses dados alimentam a fila do gestor. Não é um chat solto. | `ia/TriagemIaClient.java` |
| 5 | **Arquitetura distribuída: opção A** (20) | RabbitMQ:<ul><li>A Java **publica e consome** `ocorrencia.criada` (triagem assíncrona), com retry e DLQ.</li><li>A Java **consome** `apoio.*`, publicado pela API .NET.</li></ul> | `messaging/` |
| 6 | **Documentação e Mobile** (10) | Swagger com Authorize, este README e o contrato JSON para o app (seção 9) | `/swagger-ui.html` |

---

## 3. Como executar

### Pré-requisitos

- JDK 21
- Docker (para PostgreSQL e RabbitMQ)
- Maven: não precisa instalar, o projeto traz o wrapper (`mvnw`)

### Passo a passo

```bash
# 1. Banco e broker
docker compose up -d

# 2. Aplicação (Windows: mvnw.cmd no PowerShell é .\mvnw.cmd)
./mvnw spring-boot:run
```

Na primeira execução, o Flyway cria as tabelas (`V1`) e insere os dados de demonstração (`V2`).

| Recurso | Endereço |
|---|---|
| API | <http://localhost:8080> |
| Swagger | <http://localhost:8080/swagger-ui.html> |
| Saúde | <http://localhost:8080/actuator/health> |
| Painel do RabbitMQ | <http://localhost:15672> (`guest` / `guest`) |

**Sem chave de IA, a aplicação sobe normalmente.** A triagem usa as regras de fallback (`origemTriagem: "REGRAS"`). Para ligar a IA, defina as variáveis do provedor antes de rodar:

| Provedor | `AI_BASE_URL` | `AI_MODEL` | Custo |
|---|---|---|---|
| **Groq** (usado no deploy) | `https://api.groq.com/openai` | `openai/gpt-oss-120b` | Gratuito, sem cartão (chave em console.groq.com) |
| ChatGPT (OpenAI) | `https://api.openai.com` (padrão) | `gpt-4o-mini` | Pago (platform.openai.com/api-keys) |
| DeepSeek | `https://api.deepseek.com` | `deepseek-v4-flash` | Pago (platform.deepseek.com) |

```bash
# Linux / macOS (no Windows PowerShell: $env:AI_API_KEY="...")
export AI_API_KEY="gsk_..."
export AI_BASE_URL="https://api.groq.com/openai"
export AI_MODEL="openai/gpt-oss-120b"
./mvnw spring-boot:run
```

> Modelos são aposentados com frequência: o DeepSeek desligou `deepseek-chat` e `deepseek-reasoner` em 24/07/2026 (use `deepseek-v4-flash`), e a Groq desligou o `llama-3.3-70b-versatile` em 16/08/2026 (use `openai/gpt-oss-120b`). Como o modelo vem de variável de ambiente, a troca não exige mudar código.

### Variáveis de ambiente

Nenhum segredo está no repositório. O [`.env.example`](.env.example) lista todas as variáveis.

| Variável | Padrão local | Para quê |
|---|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | PostgreSQL do docker-compose | Banco |
| `RABBITMQ_URL` | `amqp://guest:guest@localhost:5672` | Broker (o mesmo da API .NET) |
| `JWT_SECRET` | valor de desenvolvimento | Assinatura HS256, **igual à da API .NET**, com 32 ou mais caracteres |
| `JWT_ISSUER` / `JWT_AUDIENCE` | `consertaai` / `consertaai-app` | Iguais aos da API .NET |
| `AI_API_KEY` | vazio (usa as regras) | Chave do Groq, do ChatGPT ou do DeepSeek |
| `AI_BASE_URL` / `AI_MODEL` | `https://api.openai.com` / `gpt-4o-mini` | Provedor e modelo |

---

## 4. Usuários de teste

Todos usam a senha **`senha123`**.

| Papel | E-mail | Uso na demonstração |
|---|---|---|
| CIDADAO | `maria@consertaai.com` | Abre ocorrências e acompanha as suas |
| CIDADAO | `joao@consertaai.com` | Segundo cidadão |
| GESTOR | `gestor@consertaai.com` | Fila priorizada, mudança de status, categorias |

O banco já vem com 5 ocorrências triadas, em bairros e estados diferentes. A ocorrência `1` existe de propósito, porque os exemplos da API .NET usam esse id.

---

## 5. Endpoints principais

| Método e rota | Acesso | Descrição | Respostas |
|---|---|---|---|
| `POST /auth/login` | público | Devolve `token`, `expiraEm` e `usuario` | 200, 400, 401 |
| `POST /auth/registro` | público | Cadastra um cidadão e já devolve o token | 201, 400, 409 |
| `GET /auth/me` | autenticado | Dados do dono do token | 200, 401 |
| `GET /ocorrencias?status=&bairro=&categoriaId=&page=&size=&sort=` | público | Lista paginada com filtros | 200, 400 |
| `GET /ocorrencias/{id}` | público | Detalhe, incluindo o resultado da triagem | 200, 404 |
| `GET /ocorrencias/{id}/historico` | público | Linha do tempo de status | 200, 404 |
| `GET /ocorrencias/minhas` | CIDADAO | Ocorrências do cidadão logado | 200 |
| `POST /ocorrencias` | CIDADAO | Abre a ocorrência em `EM_TRIAGEM` e dispara a triagem por IA | 201, 400, 401, 403, 422 |
| `PUT /ocorrencias/{id}` | autor | Edita enquanto está em triagem ou aberta | 200, 400, 403, 404, 422 |
| `DELETE /ocorrencias/{id}` | autor ou GESTOR | Exclui | 204, 403, 404, 422 |
| `GET /ocorrencias/fila` | GESTOR | Em aberto, ordenada por prioridade | 200, 403 |
| `PATCH /ocorrencias/{id}/status` | GESTOR | Muda o status e grava no histórico | 200, 400, 403, 404, 422 |
| `POST /ocorrencias/{id}/triagem` | GESTOR | Refaz a triagem por IA na hora | 200, 403, 404 |
| `GET /categorias` | público | Categorias ativas | 200 |
| `GET /categorias/todas` | GESTOR | Todas as categorias, paginadas | 200 |
| `POST/PUT/DELETE /categorias` | GESTOR | CRUD de categorias | 201/200/204, 409 |
| `PATCH /internal/ocorrencias/{id}/apoios` | autenticado | Plano B da API .NET (HTTP no lugar do RabbitMQ) | 204, 404 |
| `GET /actuator/health` | público | Saúde (usada pelo health check da .NET) | 200 |

### Ciclo de vida da ocorrência

```
EM_TRIAGEM ──► ABERTA ──► EM_ANDAMENTO ──► RESOLVIDA
     │            │              │
     └────────────┴──────────────┴──────► CANCELADA
```

- A passagem de `EM_TRIAGEM` para `ABERTA` é feita pelo sistema, ao fim da triagem.
- As demais passagens são feitas pelo gestor.
- Uma transição fora do diagrama responde **422** e lista as permitidas.

### Prioridade da fila (0 a 100)

| Componente | Pontos |
|---|---|
| Urgência sugerida pela IA | BAIXA 15 · MEDIA 30 · ALTA 50 · CRITICA 65 |
| Risco imediato | +15 |
| Apoios dos cidadãos (vindos da API .NET) | +2 por apoio, até +15 |
| Dias em aberto | +1 por dia, até +5 |

A IA sugere a urgência e o risco, mas quem ordena a fila é essa conta. Ela é determinística e o gestor consegue auditar cada número.

---

## 6. Autenticação JWT

```
1. POST /auth/login {email, senha}
2. A API busca o usuário e confere a senha com BCrypt.
3. O TokenService monta e assina o JWT (HS256, segredo JWT_SECRET):
   iss=consertaai  aud=consertaai-app  sub=<id>  roles=["CIDADAO"]  exp=+120 min
4. O app guarda o token e envia em toda requisição: Authorization: Bearer <token>
5. O BearerTokenAuthenticationFilter valida assinatura, emissor, audiência e
   expiração, e transforma o claim roles em ROLE_CIDADAO / ROLE_GESTOR.
6. As regras de SecurityConfig liberam ou barram a rota (401 sem token, 403 com papel errado).
```

- **Sem sessão:** cada requisição se autentica sozinha (stateless), então não há CSRF.
- **O mesmo token vale na API .NET:** as duas usam o mesmo segredo, emissor e audiência. O app faz login uma vez só.
- **Quem é o usuário** vem sempre do claim `sub` do token, nunca do corpo da requisição. Um cidadão não consegue editar a ocorrência de outro trocando um id.
- **Cadastro público cria sempre um `CIDADAO`.** Gestores são criados pela prefeitura.

---

## 7. Funcionalidade com Spring AI: triagem automática

**Problema que resolve:** o cidadão escreve do jeito dele ("tem um buraco enorme aqui na frente, os carros tão indo pela contramão"). Sem triagem, o gestor lê tudo, classifica, decide a urgência e procura repetidas, uma por uma.

**Como funciona:**

1. `POST /ocorrencias` grava a ocorrência como `EM_TRIAGEM` e responde **201 na hora**.
2. Depois do commit, o evento `ocorrencia.criada` vai para o RabbitMQ.
3. O `TriagemConsumer` monta o contexto da triagem:
   - as categorias disponíveis, com o órgão de cada uma;
   - a descrição do cidadão;
   - até 15 ocorrências já abertas no mesmo bairro, que são as candidatas a duplicada.
4. O `TriagemIaClient` chama o modelo com o `ChatClient` do Spring AI e pede a resposta como um record Java (`.entity(RespostaIa.class)`). O Spring AI gera o JSON Schema e converte a resposta.
5. O resultado é gravado e a ocorrência passa para `ABERTA`, já com a prioridade calculada.

**O que o modelo devolve:** categoria, urgência, risco imediato, órgão responsável, um resumo objetivo para o gestor e o id de uma ocorrência que pareça ser o mesmo problema. Com esse id, o app pode sugerir **apoiar** a existente, pela API .NET, em vez de criar uma duplicada.

**Cuidados:**

| Risco | Tratamento |
|---|---|
| Chamada à IA é lenta (segundos) | Roda fora da requisição (fila) e fora de transação: lê os dados, chama a IA e grava o resultado |
| IA fora do ar, sem chave ou resposta inválida | `TriagemRegras`: palavras-chave e similaridade de texto. A ocorrência nunca fica presa em triagem |
| Modelo inventa um id de duplicada | Só é aceito um id que estava entre as candidatas enviadas |
| Prompt injection na descrição | As instruções de sistema mandam tratar o texto como dado. Chaves `{ }` do texto do usuário são neutralizadas |
| Categoria escrita diferente ("iluminacao publica") | Comparação sem acento e sem caixa. Nome desconhecido cai em "Outros" |

**Provedor:** o código usa o cliente do Spring AI compatível com OpenAI, como na aula 25 (ChatGPT e DeepSeek). O deploy usa a **Groq** com o modelo `openai/gpt-oss-120b` (o modelo aberto da própria OpenAI), que fala o mesmo protocolo e tem plano gratuito. Trocar para ChatGPT ou DeepSeek é só mudar as variáveis `AI_BASE_URL` e `AI_MODEL`; nenhuma linha de código muda.

**Versão do Spring AI:** o exemplo da aula usa a 0.8.1 (`OpenAiChatClient.call(texto)`). Este projeto usa a **1.0**, em que a API passou a ser o `ChatClient` fluente. Com ele vem a **saída estruturada**: `.entity(RespostaIa.class)` devolve um objeto Java, e não um texto livre que precisaria ser interpretado.

**Retry:** `spring.ai.retry.max-attempts: 2`, seguindo o exemplo da aula. Num erro 429 (limite de uso) ou 5xx, o Spring AI tenta só mais uma vez e a triagem cai nas regras, em vez de ficar minutos esperando o provedor.

---

## 8. Arquitetura distribuída: opção A (RabbitMQ)

**Opção escolhida no requisito 5: A (RabbitMQ).**

```
              exchange TOPIC "consertaai.eventos" (compartilhada com a API .NET)
               │                                       │
  "ocorrencia.criada" (Java publica)          "apoio.*" (.NET publica)
               ▼                                       ▼
   fila consertaai.java.triagem              fila consertaai.apoios
   └─ DLQ consertaai.java.triagem.dlq                  │
               │                                       │
        TriagemConsumer                          ApoiosConsumer
     (Spring AI classifica)               (atualiza apoios e prioridade)
```

| Decisão | Motivo |
|---|---|
| Publicar só **depois do commit** (`@TransactionalEventListener(AFTER_COMMIT)`) | Se a transação falhar, nenhum evento sai sobre uma ocorrência que não existe |
| Publicar em **outra thread** (`@Async`) | O cidadão não espera o broker |
| **Retry** de 3 tentativas (2 s, 4 s) e **DLQ** na fila de triagem | Uma mensagem com problema não trava a fila nem se perde |
| Broker fora do ar na publicação: **triagem direta** | A ocorrência é triada mesmo sem fila |
| Evento de apoio traz o **total**, e não +1/−1 | Aplicar duas vezes dá o mesmo resultado (idempotente) |
| Evento de apoio **mais antigo que o último aplicado** é descartado | Mensagens fora de ordem não sobrescrevem um total mais novo |
| Fila `consertaai.apoios` declarada **igual à da .NET** (durável, sem argumentos) | O RabbitMQ recusa redeclarar uma fila com argumentos diferentes |
| JSON com **pacotes confiáveis** | O cabeçalho de tipo não consegue instanciar classes arbitrárias |

**Para ver funcionando:** abra o painel em <http://localhost:15672>, aba *Queues*, abra uma ocorrência e acompanhe a mensagem passar pela fila. Os logs mostram `Evento ocorrencia.criada publicado` e depois `Ocorrencia N triada por IA`.

---

## 9. Integração com o app Mobile

**URL base:** a do deploy (seção de links) ou `http://<IP-da-máquina>:8080` na rede local.

### Login

```http
POST /auth/login
Content-Type: application/json

{ "email": "maria@consertaai.com", "senha": "senha123" }
```
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tipo": "Bearer",
  "expiraEm": "2026-11-20T15:00:00Z",
  "usuario": { "id": 1, "nome": "Maria Souza", "email": "maria@consertaai.com", "papel": "CIDADAO", "bairro": "Centro" }
}
```

Guarde o `token` (por exemplo, no `SecureStore`) e envie `Authorization: Bearer <token>` nesta API **e na API .NET**.

### Abrir ocorrência (CIDADAO)

```http
POST /ocorrencias
Authorization: Bearer <token>
Content-Type: application/json

{
  "titulo": "Buraco na frente da escola",
  "descricao": "Buraco grande na faixa de pedestres, os carros desviam pela contramão",
  "endereco": "Rua Nereu Ramos, 350",
  "bairro": "Centro",
  "latitude": -27.0987,
  "longitude": -52.6123,
  "categoriaId": null
}
```

A resposta é **201**, com `status: "EM_TRIAGEM"`. Alguns segundos depois, `GET /ocorrencias/{id}` traz a triagem:

```json
{
  "id": 6,
  "titulo": "Buraco na frente da escola",
  "status": "ABERTA",
  "categoria": { "id": 1, "nome": "Buraco na via" },
  "urgencia": "ALTA",
  "riscoImediato": true,
  "resumoIa": "Buraco em faixa de pedestres em frente a escola, com veículos invadindo a contramão.",
  "orgaoSugerido": "Secretaria de Obras",
  "origemTriagem": "IA",
  "possivelDuplicadaDeId": 1,
  "qtdApoios": 0,
  "prioridade": 65,
  "prazoAtendimento": "2026-11-29",
  "autor": { "id": 1, "nome": "Maria Souza" },
  "criadoEm": "2026-11-19T10:12:03"
}
```

**Sugestão de tela:** se `possivelDuplicadaDeId` vier preenchido, mostre "Parece que já reportaram isso. Quer apoiar a ocorrência #1?". O apoio é feito com `POST /apoios` na API .NET.

### Listas paginadas

Todas as listas vêm no mesmo envelope:

```json
{ "conteudo": [ ... ], "pagina": 0, "tamanho": 10, "totalElementos": 23, "totalPaginas": 3, "ultima": false }
```

### Erros

Todos os erros vêm no mesmo formato. Mostre o campo `mensagem` ao usuário e, em erro 400, os itens de `campos`:

```json
{
  "status": 400,
  "erro": "Dados inválidos",
  "mensagem": "Um ou mais campos não passaram na validação.",
  "caminho": "/ocorrencias",
  "campos": [ { "campo": "descricao", "mensagem": "A descrição deve ter entre 10 e 1000 caracteres" } ]
}
```

### Telas e endpoints

| Tela do app | Endpoints |
|---|---|
| Login e cadastro | `POST /auth/login`, `POST /auth/registro` |
| Mapa/lista da cidade | `GET /ocorrencias?bairro=Centro&status=ABERTA` |
| Nova ocorrência | `GET /categorias`, `POST /ocorrencias` |
| Minhas ocorrências | `GET /ocorrencias/minhas` |
| Detalhe e acompanhamento | `GET /ocorrencias/{id}`, `GET /ocorrencias/{id}/historico` |
| Editar ou excluir | `PUT /ocorrencias/{id}`, `DELETE /ocorrencias/{id}` |
| Fila do gestor | `GET /ocorrencias/fila`, `PATCH /ocorrencias/{id}/status` |

---

## 10. Testes

```bash
./mvnw test
```

| Teste | Verifica |
|---|---|
| `ApiIntegrationTest` | Contexto completo com H2: login, JWT, 401 sem token, 403 por papel, 201 ao abrir, 400 com campos, 404 |
| `TriagemServiceTest` | Usa a IA quando ela responde; cai nas regras quando falha ou não tem chave |
| `TriagemRegrasTest` | Classificação por palavra-chave, criticidade e detecção de duplicada |
| `OcorrenciaApoiosTest` | Evento de apoio idempotente, evento atrasado descartado, ocorrência inexistente |
| `PrioridadeCalculatorTest` | Cálculo e tetos da prioridade |
| `StatusOcorrenciaTest` | Transições permitidas e proibidas |

Os testes não precisam de PostgreSQL, RabbitMQ nem chave de IA.

---

## 11. Deploy no Render

1. **Banco:** *New → PostgreSQL* (plano Free). Anote o hostname externo, o database, o usuário e a senha.
2. **Broker:** crie uma instância gratuita *Little Lemur* em <https://www.cloudamqp.com> e copie a AMQP URL (`amqps://...`). **Use a mesma URL na API .NET**, para os eventos de apoio chegarem aqui.
3. **Aplicação:** *New → Web Service*, conecte este repositório e escolha **Docker** (o `Dockerfile` está na raiz).
4. Em *Environment*, crie as variáveis:

| Variável | Valor |
|---|---|
| `DB_URL` | `jdbc:postgresql://<hostname-externo>:5432/<database>?sslmode=require` |
| `DB_USERNAME` / `DB_PASSWORD` | do banco |
| `RABBITMQ_URL` | URL do CloudAMQP |
| `JWT_SECRET` | o mesmo segredo da API .NET |
| `AI_API_KEY` | chave da IA (Groq: `gsk_...`) |
| `AI_BASE_URL` | `https://api.groq.com/openai` |
| `AI_MODEL` | `openai/gpt-oss-120b` |

5. Faça o deploy. No log devem aparecer `Migrating schema "public" to version "2 - seed dados demonstracao"` e `Started ConsertaAiApplication`.
6. Na API .NET, defina `ApiJava__BaseUrl` com a URL do Render.

> O `DB_URL` precisa começar com `jdbc:postgresql://`. Usuário e senha não entram na URL.

---

## 12. Integrantes

| RM | Nome | Turma |
|---|---|---|
| 556649 | Matheus Arazin de Oliveira | 2TDSPV |
| 565597 | Artur Pioli Silva | 2TDSPV |
| 563454 | Kevin Martins Campos | 2TDSPV |
| 566058 | Pedro Gabriel Claes | 2TDSPV |

---

## 13. Declaração de uso de IA

**Concepção: grupo.** A ideia do Conserta Aí, a divisão entre as APIs Java e .NET, o contrato do JWT e dos eventos no RabbitMQ, e a escolha da triagem como funcionalidade de IA foram definidas pelo grupo.

**Ferramenta de apoio: Claude Code (Anthropic).** Usada como assistente de programação para:
- gerar a primeira versão do código e dos testes a partir das definições do grupo;
- escrever este README.

**Validação: `PREENCHER` pelo grupo.** Descrever o que foi revisado, executado e testado, por exemplo: compilação e testes locais, fluxo completo no Swagger, integração com a API .NET e com o app.

**IA dentro do produto:** a triagem usa um LLM (gpt-oss-120b na Groq; ChatGPT ou DeepSeek por configuração) via Spring AI. A sugestão da IA nunca decide sozinha:
- a prioridade é uma conta auditável;
- o gestor muda o status;
- se a IA falhar, entram as regras de fallback.
