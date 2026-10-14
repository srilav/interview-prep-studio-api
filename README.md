# Prep Studio API

Backend for [Interview Prep Studio](https://github.com/srilav/interview-prep-studio): look up interview keys (terms and concepts), get AI explanations with Java examples, and discuss them live with other learners and an AI tutor.

- **Java 21, Spring Boot 3.3**, modular monolith with one Maven module per bounded context (DDD)
- **gRPC** API (browsers reach it as gRPC-Web through Envoy)
- **STOMP over WebSocket** for live discussion
- **Domain events** written to a transactional outbox, delivered in-process and relayed to Kafka (Redpanda locally)
- **Claude Haiku 5.5** (`claude-haiku-5-5`) behind an anti-corruption layer

The React frontend is in [interview-prep-studio-web](https://github.com/srilav/interview-prep-studio-web). The full design is in [DESIGN.md](https://github.com/srilav/interview-prep-studio/blob/main/docs/DESIGN.md).

## Modules

| Module | Bounded context | What it holds |
|---|---|---|
| `proto/` | API contract | `.proto` files, the single source of truth (the frontend generates its client from here) |
| `api-contracts` | | Java stubs generated from `proto/` |
| `shared-kernel` | | `AggregateRoot`, `DomainEvent`, `DomainEventPublisher`, outbox and relays |
| `ai-gateway` | | `LlmGateway` port, Claude adapter, offline fake |
| `catalog` | Catalog | Taxonomy (`Platform` › `Category` › `Subcategory`) and `Key` aggregates, admin and lookup gRPC services |
| `learning` | Learning | `Explanation` aggregate, streaming generation |
| `discussion` | Discussion | `DiscussionThread` aggregate, STOMP controller, AI tutor |
| `progress` | Progress | Study-activity read model projected from other contexts' events |
| `app` | | Spring Boot entry point, Flyway migrations, gRPC error mapping |

Each context follows `api → application → domain`. Other contexts read the Catalog only through `CatalogQueries` (a published snapshot API), never its aggregates.

## Domain events

| Context | Events |
|---|---|
| Catalog | `PlatformAdded`, `CategoryAdded`, `SubcategoryAdded`, `KeyCreated`, `KeyUpdated`, `KeyMoved`, `KeyPublished`, `KeyDeleted` |
| Learning | `ExplanationRequested`, `ExplanationGenerated`, `ExplanationGenerationFailed`, `ExplanationViewed` |
| Discussion | `ThreadOpened`, `MessagePosted`, `AiReplyRequested` |

Aggregates collect events; application services hand them to `DomainEventPublisher` inside the same transaction. It writes each to `outbox_event` and publishes it as a Spring event. In-process listeners (`ThreadBroadcaster`, `AiTutor`, `ProgressProjection`) run after commit. With `KAFKA_ENABLED=true`, a relay sends outbox rows to the topics `catalog.events`, `learning.events` and `discussion.events`, keyed by aggregate id.

## Run locally

Requires JDK 21 and Docker.

```bash
docker compose up -d                 # Postgres on 5432, Envoy (gRPC-Web) on 8081
export ANTHROPIC_API_KEY=sk-ant-...  # or LLM_PROVIDER=fake to run offline
./mvnw -pl app -am spring-boot:run
```

| Port | What |
|---|---|
| 9090 | gRPC |
| 8081 | gRPC-Web via Envoy (what the browser uses) |
| 8080 | WebSocket, STOMP endpoint `/ws` |

Flyway creates the schema and loads sample content (Java, Spring Boot and Kafka keys).

To relay events to Kafka:

```bash
docker compose --profile kafka up -d
KAFKA_ENABLED=true ./mvnw -pl app -am spring-boot:run
```

### Configuration

| Variable | Default | |
|---|---|---|
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/prepstudio` | also `DATABASE_USER`, `DATABASE_PASSWORD` |
| `LLM_PROVIDER` | `claude` | `fake` returns canned text, for offline work |
| `LLM_MODEL` | `claude-haiku-5-5` | |
| `KAFKA_ENABLED` | `false` | events are always delivered in-process |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:19092` | |
| `ALLOWED_ORIGINS` | `http://localhost:5173` | WebSocket origins |

## WebSocket protocol

- Send to `/app/threads/{threadId}/messages`: `{"author": "Ada", "content": "@tutor why ...?"}`. Mentioning `@tutor` asks the AI tutor to reply (5 requests per learner per minute).
- Subscribe to `/topic/threads/{threadId}`. Frames:
  - `{"type":"message","message":{id, threadId, author, fromAiTutor, content, postedAt}}`
  - `{"type":"ai_chunk","threadId","replyId","delta"}`: the tutor's reply as it streams; the final `message` reuses `replyId` as its id
  - `{"type":"ai_error","threadId","replyId","error"}`
  - `{"type":"error","threadId","error"}`: the post was rejected (for example, too long)

## Tests

```bash
./mvnw verify
```

Unit tests cover the aggregates' rules and events. `PrepStudioApiTest` boots the whole app with in-process gRPC, H2 and the fake LLM, and drives the taxonomy, search, explanation streaming, outbox, AI tutor and progress through the gRPC API.

## Not done yet

- **Authentication:** learners pass a display name; admin RPCs are open. Planned: Keycloak JWT checked by a gRPC interceptor, with an `editor` role for `TaxonomyAdminService`.
- **Scaling discussion:** the STOMP broker is in-memory, so one instance only. Use a broker relay (RabbitMQ STOMP or Redis) before scaling out.
- **Tree editing:** no rename, reorder or delete for taxonomy nodes yet.
