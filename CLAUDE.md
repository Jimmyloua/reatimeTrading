<!-- GSD:project-start source:PROJECT.md -->
## Project

**Real-Time Trading Platform**

A second-hand digital device marketplace where users can buy and sell electronics with real-time communication and transaction capabilities. The platform supports all categories of digital devices - from consumer electronics (phones, laptops, cameras) to professional equipment (servers, networking gear). Users act as both buyers and sellers in a peer-to-peer marketplace with escrow-secured transactions.

**Core Value:** Safe, transparent peer-to-peer transactions for second-hand digital devices with real-time communication and reputation-based trust.

### Constraints

- **Tech Stack**: Spring Boot + JDK 21 backend, React frontend, MySQL, Redis, Kafka — mandated by user
- **Architecture**: Must support real-time features (WebSocket/Server-Sent Events for chat, notifications, live updates)
- **Performance**: Low-latency messaging for real-time features
- **Scalability**: Architecture should support horizontal scaling for chat/notification services
<!-- GSD:project-end -->

<!-- GSD:stack-start source:research/STACK.md -->
## Technology Stack

## Recommended Stack
### Core Technologies (Mandated by Project)
| Technology | Version | Purpose | Why Recommended |
|------------|---------|---------|-----------------|
| Spring Boot | 3.5.x | Backend framework | Mandated. Virtual threads support (Project Loom) for high-concurrency real-time features. Spring Security 7.x, Spring Data 4.x included. |
| JDK | 21 LTS | Runtime | Mandated. Virtual threads, pattern matching, records. LTS support until 2031. |
| React | 19.x | Frontend framework | Mandated. React 19 adds Server Components, Actions, and improved concurrent rendering ideal for real-time updates. |
| MySQL | 8.x | Primary database | Mandated. Mature, widely supported. JSON columns for flexible item attributes. Spatial indexes for geo-location queries. |
| Redis | 7.x | Cache, sessions, pub/sub | Mandated. Session storage, cache layer, real-time pub/sub for notifications, rate limiting. |
| Apache Kafka | 4.x | Event streaming | Mandated. High-throughput message broker for chat messages, notifications, event sourcing, async processing. |
### Backend Supporting Libraries
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Spring Data JPA | 4.0.x | ORM and data access | All database operations. Use derived queries for simple queries, @Query for complex. |
| Spring Data Redis | 4.0.x | Redis integration | Session storage, caching, pub/sub for real-time notifications. |
| Spring Kafka | 4.0.x | Kafka integration | Message producer/consumer for chat, notifications, async events. |
| Spring Security | 7.0.x | Authentication/authorization | JWT-based auth, OAuth2 for social login, method-level security. |
| Spring Session | 3.5.x | Session management | Distributed sessions in Redis for horizontal scaling. |
| Spring WebSocket | Included | Real-time bidirectional communication | Chat, live notifications, activity indicators. Use STOMP protocol. |
| JJWT | 0.13.x | JWT token handling | Generate/validate JWTs for stateless authentication. |
| MapStruct | 1.6.x | DTO mapping | Clean separation between entities and API responses. Reduces boilerplate. |
| Lettuce | 7.5.x | Redis client | Async Redis operations. Spring Boot default Redis client. |
| HikariCP | Included | Connection pooling | Production-ready connection pool. Auto-configured by Spring Boot. |
| Liquibase | 12.x OR Flyway | Database migrations | Schema versioning and migrations. Liquibase for XML/YAML, Flyway for SQL-only. |
| SpringDoc OpenAPI | 3.0.x | API documentation | Auto-generates OpenAPI 3.x spec from code. Swagger UI included. |
| Micrometer | 1.16.x | Metrics | Prometheus-compatible metrics. Integrates with Spring Boot Actuator. |
| Testcontainers | 2.0.x | Integration testing | Docker-based integration tests for MySQL, Redis, Kafka. |
### Frontend Supporting Libraries
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| TypeScript | 5.9.x | Type safety | All frontend code. Strict mode recommended. |
| Vite | 8.x | Build tool | Development server and production builds. Fast HMR. |
| TanStack Query | 5.x | Server state management | All API calls. Caching, background refetching, optimistic updates. |
| Zustand | 5.x | Client state management | UI state, filters, user preferences. Lighter than Redux. |
| React Hook Form | 7.x | Form handling | Listing forms, search filters. Performant, minimal re-renders. |
| Zod | 4.x | Schema validation | Form validation, API response validation. TypeScript-first. |
| Axios | 1.13.x | HTTP client | REST API calls. Interceptors for auth tokens, error handling. |
| @stomp/stompjs | 7.x | WebSocket client | Real-time chat, notifications. STOMP protocol over WebSocket. |
| sockjs-client | 1.6.x | WebSocket fallback | Fallback transport for restricted networks. Use with STOMP. |
| React Dropzone | 15.x | File uploads | Item image uploads. Drag-and-drop interface. |
| Leaflet + React Leaflet | 1.9.x + 5.x | Maps | Geo-location display, item discovery by location. Open-source alternative to Google Maps. |
| Lucide React | 0.577.x | Icons | Lightweight icon library. Tree-shakeable. |
| TailwindCSS | 4.x | Styling | Utility-first CSS. Rapid prototyping, consistent design system. |
| clsx + tailwind-merge | 2.x + 3.x | Conditional classes | Dynamic class composition with Tailwind. |
| Framer Motion | 12.x | Animations | UI transitions, micro-interactions. Improves perceived performance. |
### Development Tools
| Tool | Purpose | Notes |
|------|---------|-------|
| Spring Boot DevTools | Hot reload | Development only. Auto-restart on classpath changes. |
| ESLint | Linting | Use flat config (eslint.config.js). Airbnb or standard preset. |
| Prettier | Code formatting | Integrate with ESLint. Format on save. |
| Vitest | Unit testing | Vite-native test runner. Fast, ESM-first. |
| Testing Library | React component testing | User-centric testing approach. Integration with Vitest. |
| Playwright | E2E testing | Cross-browser testing. Visual regression optional. |
| Docker Compose | Local development | MySQL, Redis, Kafka containers. Consistent dev environment. |
### Infrastructure (Recommended)
| Technology | Purpose | Why Recommended |
|------------|---------|-----------------|
| AWS S3 (or compatible) | Image storage | Scalable, CDN integration. Use presigned URLs for secure uploads. |
| AWS CloudFront (or CDN) | Static assets | Global distribution for images, frontend assets. |
| Nginx | Reverse proxy | SSL termination, static file serving, load balancing. |
| Prometheus + Grafana | Monitoring | Metrics collection and visualization. Micrometer integration. |
| ELK Stack or Loki | Logging | Centralized logging. Structured JSON logs from Spring Boot. |
## Installation
### Backend (Maven pom.xml)
### Frontend (package.json)
## Alternatives Considered
| Recommended | Alternative | When to Use Alternative |
|-------------|-------------|-------------------------|
| Zustand | Redux Toolkit | Large teams with complex state, need time-travel debugging, middleware ecosystem |
| Zustand | Jotai | Atomic state model preferred, minimal boilerplate, fine-grained reactivity |
| TanStack Query | SWR | Simpler use cases, Vercel ecosystem, less configuration needed |
| TanStack Query | Apollo Client | If using GraphQL instead of REST |
| Liquibase | Flyway | SQL-only migrations preferred, simpler syntax, team familiarity |
| MySQL | PostgreSQL | More complex queries needed, JSONB better performance, extension ecosystem |
| Redis | Valkey | Cost optimization, AWS ElastiCache Valkey mode |
| STOMP over WebSocket | Server-Sent Events (SSE) | One-way real-time updates only (notifications), simpler protocol |
| STOMP over WebSocket | gRPC Streaming | Microservices architecture, need bidirectional streaming between services |
| Axios | fetch API | Minimal HTTP needs, avoid dependency, native streaming |
| Leaflet | Mapbox GL | Advanced map features needed, vector tiles, custom styling |
| TailwindCSS | CSS Modules | Team prefers traditional CSS, design system already exists |
| TailwindCSS | Styled Components | CSS-in-JS preferred, dynamic styling based on props |
## What NOT to Use
| Avoid | Why | Use Instead |
|-------|-----|-------------|
| Spring MVC with JSP | Outdated, no modern frontend integration | React SPA with Spring REST API |
| Thymeleaf for dynamic content | Limited interactivity, poor fit for real-time | React with WebSocket/STOMP |
| Redux for simple state | Boilerplate overkill for most use cases | Zustand for client state, TanStack Query for server state |
| REST for chat | Polling overhead, not truly real-time | WebSocket with STOMP |
| LocalStorage for auth tokens | XSS vulnerability | HttpOnly cookies with CSRF protection, or secure session storage |
| Synchronous Kafka consumers | Blocks threads, defeats async purpose | @KafkaListener with async processing |
| Polling for notifications | Wasteful, poor UX | Server-Sent Events or WebSocket push |
| JWT in URL parameters | Security risk (logging, history) | Authorization header or secure cookie |
| Embedded H2 for production | Not production-ready, data loss risk | MySQL with proper backup strategy |
| Singleton services | Not cluster-safe, race conditions | Use Redis for distributed locks and session |
| Callbacks for async | Hard to reason about, nested complexity | Project Loom virtual threads, CompletableFuture, reactive streams |
## Stack Patterns by Variant
- Use Spring Boot 3.5.x with virtual threads enabled (`spring.threads.virtual.enabled=true`)
- Start with React 19 + Vite + TypeScript strict mode
- Use TanStack Query for all server state, Zustand only for UI state
- Wire WebSocket/STOMP from day one for real-time features
- Add Spring WebSocket starter without changing REST endpoints
- Use STOMP for structured messaging (subscribe/publish model)
- React frontend connects via SockJS with WebSocket fallback
- Kafka for message persistence and replay capability
- Spring Session with Redis for distributed sessions
- Kafka partitions for chat message ordering per conversation
- Redis pub/sub for real-time notification fan-out
- Consider event sourcing pattern for audit trail
## Version Compatibility
| Package A | Compatible With | Notes |
|-----------|-----------------|-------|
| Spring Boot 3.5.x | JDK 21+ | Virtual threads require JDK 21 |
| Spring Boot 3.5.x | Spring Security 7.x | Included in BOM |
| Spring Boot 3.5.x | Spring Data 4.x | Included in BOM |
| Spring Boot 3.5.x | Hibernate 7.x | Included in BOM |
| React 19.x | React DOM 19.x | Must match versions |
| React 19.x | TanStack Query 5.x | Compatible |
| TanStack Query 5.x | React 18.x or 19.x | Works with both |
| React Leaflet 5.x | React 18.x+ | Requires React 18+ |
| React Leaflet 5.x | Leaflet 1.9.x | Must use Leaflet 1.9.x |
| SpringDoc OpenAPI 3.x | Spring Boot 3.x | Requires Spring Boot 3 |
| MapStruct 1.6.x | JDK 21 | Compatible |
| Testcontainers 2.x | JDK 21 | Compatible |
## Sources
- GitHub API - Current release versions verified for Spring Boot, React, Spring Security, Spring Data, Spring Kafka, MapStruct, Liquibase, Micrometer, Testcontainers, JJWT
- npm Registry - Current versions verified for React, TanStack Query, Zustand, React Hook Form, Zod, Axios, STOMP.js, SockJS, React Dropzone, Leaflet, TailwindCSS, TypeScript, Vite, Vitest
- Spring Boot 3.x Documentation - Virtual threads, Spring Security 7.x, Spring Data 4.x integration patterns
- Project Requirements - Mandated technology stack (Spring Boot + JDK 21, React, MySQL, Redis, Kafka)
- Confidence: HIGH - All versions verified directly from package registries and GitHub release APIs on 2026-03-21
<!-- GSD:stack-end -->

<!-- GSD:conventions-start source:CONVENTIONS.md -->
## Conventions

Conventions not yet established. Will populate as patterns emerge during development.
<!-- GSD:conventions-end -->

<!-- GSD:architecture-start source:ARCHITECTURE.md -->
## Architecture

Architecture not yet mapped. Follow existing patterns found in the codebase.
<!-- GSD:architecture-end -->

<!-- GSD:workflow-start source:GSD defaults -->
## GSD Workflow Enforcement

Before using Edit, Write, or other file-changing tools, start work through a GSD command so planning artifacts and execution context stay in sync.

Use these entry points:
- `/gsd:quick` for small fixes, doc updates, and ad-hoc tasks
- `/gsd:debug` for investigation and bug fixing
- `/gsd:execute-phase` for planned phase work

Do not make direct repo edits outside a GSD workflow unless the user explicitly asks to bypass it.
<!-- GSD:workflow-end -->



<!-- GSD:profile-start -->
## Developer Profile

> Profile not yet configured. Run `/gsd:profile-user` to generate your developer profile.
> This section is managed by `generate-claude-profile` -- do not edit manually.
<!-- GSD:profile-end -->
