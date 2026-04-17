# codebase-chat

> Ask natural language questions about your Java codebase and get answers grounded in your actual source code — not generic LLM responses.

No cloud APIs. No subscriptions. Runs entirely on local hardware.

---

## demo

```
Question: "how does login work?"

Answer: The login process works as follows:

1. The client sends a POST request to /login with a LoginRequest object
2. AuthController.login() receives the request and delegates to UserService.login()
3. UserService finds the user by email via userRepository.findByEmail()
4. Account status is checked — throws AccountStatusException if not ACTIVE
5. Password is verified via passwordEncoder.matches() against the stored hash
6. JWT token is generated via jwtService.generateToken() and returned
7. AuthController sets the token as an HttpOnly cookie via CookieUtil

Sources: AuthController.java · UserService.java · LoginRequest.java
```

---

## architecture

```
┌─────────────────────────────────┐
│   React / Next.js 16            │  ← Chat UI (IBM Plex Mono, #00D4FF)
│   TypeScript · Tailwind 4       │
└────────────────┬────────────────┘
                 │ HTTP
┌────────────────▼────────────────┐
│   Spring Boot 3.5 · Java 21     │  ← RAG Orchestration
│   LangChain4j 1.12.2            │
└──────────┬──────────────────────┘
           │                │
    HTTP   │                │ LangChain4j
┌──────────▼──────┐  ┌──────▼──────────┐
│  Python FastAPI │  │  Ollama         │
│  AI Worker      │  │  llama3.1:8b    │
│                 │  │  local LLM      │
│  • javalang AST │  └─────────────────┘
│  • embeddings   │
│  • pgvector     │
└──────────┬──────┘
           │
┌──────────▼──────────────────────┐
│  PostgreSQL 16 + pgvector       │  ← Vector Store
│  cosine similarity search       │
└─────────────────────────────────┘
```

### why polyglot?

Each layer uses the best tool for the job:

- **Python** — `sentence-transformers` and `javalang` AST parsing are mature Python-native libraries with no Java equivalents
- **Java/Spring Boot** — LangChain4j keeps orchestration in the enterprise stack; straightforward to integrate into existing Java systems
- **React/TypeScript** — monospace brutalist UI designed specifically for developer tooling

---

## how it works

### 1. indexing

The Python AI worker walks a Java repository and parses every `.java` file into meaningful chunks using AST analysis — splitting by class and method boundaries, not arbitrary line counts.

Each chunk is enriched with its class and method context before embedding:

```
Class: UserService
Method: login

public AuthResponse login(LoginRequest request) {
    // actual method body — complete, not truncated
}
```

Enriched chunks are converted to 384-dimension vectors using `all-MiniLM-L6-v2` and stored in pgvector.

### 2. retrieval

When a question is asked, it is embedded into the same 384-dimension vector space and pgvector performs cosine similarity search to find the most semantically relevant chunks.

### 3. generation

Retrieved chunks are formatted into a structured Markdown prompt and sent to Ollama with a system instruction to answer only from the provided context — preventing hallucination.

---

## tech stack

| Layer | Technology |
|---|---|
| Frontend | Next.js 16, TypeScript, Tailwind 4, IBM Plex Mono |
| Backend | Spring Boot 3.5, Java 21, LangChain4j 1.12.2 |
| AI Worker | Python 3.11, FastAPI, sentence-transformers |
| LLM | Ollama — llama3.1:8b (local inference) |
| Vector Store | PostgreSQL 16 + pgvector |
| Java Parser | javalang (AST-based chunking) |

---

## prerequisites

- Docker Desktop
- Java 21
- Python 3.11
- Node.js 20
- Ollama

```bash
ollama pull llama3.1:8b
```

---

## running locally

### 1. start the database

```bash
docker compose up -d
```

### 2. start the AI worker

```bash
cd ai-worker
python -m venv venv
source venv/bin/activate
pip install -r requirements.txt
cp .env.example .env  # edit with your DB credentials
uvicorn app.main:app --reload --port 8001
```

### 3. start the backend

```bash
cd backend
cp .env.example .env  # edit with your credentials
./mvnw spring-boot:run
```

### 4. start the frontend

```bash
cd frontend
npm install
cp .env.local.example .env.local
npm run dev
```

Open `http://localhost:3000`

---

## usage

1. Enter the absolute path to a Java repository and click **index**
2. Wait for indexing to complete — the chat panel will appear
3. Ask natural language questions about the codebase
4. The AI answer appears above the retrieved source chunks

---

## key design decisions

### AST-based chunking over line splitting
Splitting by class and method boundaries produces semantically complete chunks. A method split across two chunks loses context and degrades retrieval quality significantly.

### Brace-counting for true method boundaries
A brace-counting algorithm finds the real closing `}` of each method rather than using a fixed line window. `getUserById` went from 50 lines of noise to a clean 10-line chunk. Similarity scores improved measurably — `AuthController.login` jumped from 0.34 to **0.44** cosine similarity after this fix.

### Chunk enrichment before embedding
Prefixing each chunk with `Class: X\nMethod: Y` gives the embedding model explicit semantic signal, significantly improving retrieval accuracy for method-level queries.

### Prompt engineering at the data layer
The context is formatted with Markdown `###` headers between chunks — exploiting how LLMs parse section separators from their training data. The system prompt constrains the model to answer only from provided context, preventing hallucination.

### Transaction-safe re-indexing
`TRUNCATE` was replaced with a scoped `DELETE WHERE file_path = ANY(?)` wrapped in a transaction with rollback. If any insert fails, existing data is preserved. This also lays the groundwork for multi-repo support.

### Connection pooling
`ThreadedConnectionPool(min=2, max=10)` replaces raw psycopg2 connections. Connections are borrowed per request and returned in a `finally` block — no connection leaks under concurrent load.

### Security hardening
- Credentials externalized to environment variables with safe fallbacks for local dev
- Path traversal protection on `/index` — allowlist validates repo paths before filesystem access
- CORS restricted from wildcard to configured frontend origin
- NPE eliminated in `AiWorkerClient` via null checks and `Optional`
- Constructor injection throughout — no field injection, fully testable

### Separation of concerns
The Python microservice owns all ML operations. Spring Boot owns orchestration and business logic. The vector store is abstracted behind the Python service — swapping pgvector for Qdrant or Pinecone requires no Java changes.

---

## project structure

```
codebase-chat/
├── ai-worker/                  Python FastAPI — AST parsing + embeddings
│   ├── app/
│   │   ├── main.py             FastAPI entry point — /index and /query endpoints
│   │   ├── embedder.py         sentence-transformers embedding service
│   │   ├── db.py               pgvector storage, connection pool, cosine search
│   │   └── parser/
│   │       └── java_parser.py  javalang AST chunker — brace-counting boundaries
│   ├── requirements.txt
│   └── .env.example
├── backend/                    Spring Boot — RAG orchestration
│   └── src/main/java/com/cloudrishi/codebasechat/
│       ├── controller/
│       │   └── ChatController.java     REST endpoints — /api/index, /api/chat
│       ├── service/
│       │   └── ChatService.java        RAG pipeline — retrieve → augment → generate
│       ├── client/
│       │   └── AiWorkerClient.java     WebClient — calls Python microservice
│       ├── model/
│       │   ├── QueryRequest.java
│       │   ├── QueryResponse.java
│       │   ├── IndexRequest.java
│       │   └── CodeChunk.java
│       └── config/
│           └── OllamaConfig.java       LangChain4j Ollama bean
├── frontend/                   Next.js — chat UI
│   └── app/
│       ├── components/
│       │   ├── ChatPanel.tsx           Question input + answer display
│       │   ├── ChunkCard.tsx           Retrieved source chunk with similarity score
│       │   ├── Header.tsx              Branding bar
│       │   └── IndexPanel.tsx          Repo path input + index trigger
│       ├── globals.css                 Design tokens — IBM Plex Mono, #00D4FF accent
│       ├── layout.tsx
│       └── page.tsx
└── README.md
```

---

## code quality

| Metric | Score |
|---|---|
| Architecture | 8/10 |
| Clarity and naming | 8/10 |
| Documentation | 8/10 |
| Single responsibility | 7/10 |
| Error handling | 7/10 |
| Security | 7/10 |
| Project average MI | ~74 (Good) |

*Assessed against Robert Martin, Martin Fowler, Kent Beck standards.*

---

## roadmap

- **Test coverage** — JUnit 5 + Mockito for Java layer; `unittest.mock` for Python layer; target 80% line coverage
- **Hybrid search** — combine pgvector cosine similarity with PostgreSQL full-text search for better recall
- **Git webhook trigger** — auto re-index on push to main branch
- **Graph view** — visualize class/method relationships in the React frontend
- **Multi-repo support** — index and query multiple repositories simultaneously
- **Token-aware chunking** — enforce 512-token limit per chunk to prevent embedding truncation

---

## author

**cloudrishi** — Senior Backend Engineer / Technical Architect  
20+ years enterprise Java · AI/ML pivot · building in public  
[github.com/cloudrishi](https://github.com/cloudrishi)
