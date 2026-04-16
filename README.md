# codebase-chat

> AI-powered Java codebase assistant — ask natural language questions about your Spring Boot projects and get answers grounded in your actual source code.

Built as a polyglot RAG (Retrieval-Augmented Generation) system using Java, Python, and TypeScript. No cloud APIs, no OpenAI — runs entirely on local hardware.

---

## architecture

```
React/TypeScript (Next.js 16)
        ↓
Spring Boot 3.5 + LangChain4j
        ↓                    ↓
Python FastAPI          Ollama (llama3.1:8b)
(AST chunker +          local LLM inference
 sentence-transformers)
        ↓
pgvector (PostgreSQL 16)
semantic vector search
```

### why polyglot?

Each layer uses the best tool for the job:

- **Python** — `sentence-transformers` and `javalang` AST parsing are mature Python-native libraries with no Java equivalents
- **Java/Spring Boot** — LangChain4j keeps orchestration in the enterprise stack; easier to integrate into existing Java systems
- **React/TypeScript** — monospace brutalist UI designed specifically for developer tooling

---

## how it works

### 1. indexing

The Python AI worker walks a Java repository and parses every `.java` file into meaningful chunks using AST analysis — splitting by class and method boundaries rather than arbitrary line counts.

Each chunk is enriched with its class and method context before embedding:

```
Class: UserService
Method: login

public AuthResponse login(LoginRequest request) {
    // actual method body...
}
```

Enriched chunks are converted to 384-dimension vectors using `all-MiniLM-L6-v2` and stored in pgvector.

### 2. retrieval

When a question is asked, it is embedded into the same 384-dimension space and pgvector performs cosine similarity search to find the most semantically relevant chunks.

### 3. generation

Retrieved chunks are formatted into a structured prompt and sent to Ollama with a system instruction to answer only from the provided context — preventing hallucination.

---

## tech stack

| Layer | Technology |
|---|---|
| Frontend | Next.js 16, TypeScript, Tailwind 4 |
| Backend | Spring Boot 3.5, Java 21, LangChain4j 1.12.2 |
| AI Worker | Python 3.11, FastAPI, sentence-transformers |
| LLM | Ollama — llama3.1:8b (local) |
| Vector Store | PostgreSQL 16 + pgvector |
| Java Parser | javalang (AST-based chunking) |

---

## prerequisites

- Docker Desktop
- Java 21
- Python 3.11
- Node.js 20
- Ollama with `llama3.1:8b` pulled

```bash
ollama pull llama3.1:8b
```

---

## running locally

### 1. start the database

```bash
cd docker
docker compose up -d
```

### 2. start the AI worker

```bash
cd ai-worker
python -m venv venv
source venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8001
```

### 3. start the backend

```bash
cd backend
./mvnw spring-boot:run
```

### 4. start the frontend

```bash
cd frontend
npm install
npm run dev
```

Open `http://localhost:3000`

---

## usage

1. Enter the absolute path to a Java repository and click **index**
2. Wait for indexing to complete — the chat panel will appear
3. Ask natural language questions about the codebase
4. The answer is grounded in your actual source code with retrieved chunks shown below

---

## key design decisions

**AST-based chunking over line splitting**
Splitting by class and method boundaries produces semantically complete chunks. A method split across two chunks loses context and degrades retrieval quality significantly.

**Chunk enrichment before embedding**
Prefixing each chunk with `Class: X\nMethod: Y` gives the embedding model explicit semantic signal, significantly improving retrieval accuracy for method-level queries. This moved `UserService.login()` from outside the top 5 results to correctly ranked position 5 for the query *"how does login work?"*.

**Prompt constraints**
The system prompt instructs the LLM to answer only from provided context. This prevents hallucination and forces the model to say *"not found in context"* rather than inventing an answer.

**Separation of concerns**
The Python microservice owns all ML operations. Spring Boot owns orchestration and business logic. Neither knows about the other's implementation details. The vector store interface is abstracted behind the Python service, making it straightforward to swap pgvector for Qdrant or Pinecone without touching the Java layer.

**Constructor injection over field injection**
All Spring components use constructor injection for testability and immutability. Dependencies are explicit and mockable.

---

## project structure

```
codebase-chat/
├── ai-worker/                  Python FastAPI — AST parsing + embeddings
│   ├── app/
│   │   ├── main.py             FastAPI entry point — /index and /query endpoints
│   │   ├── embedder.py         sentence-transformers embedding service
│   │   ├── db.py               pgvector storage and cosine similarity search
│   │   └── parser/
│   │       └── java_parser.py  javalang AST chunker — splits by class/method
│   ├── requirements.txt
│   └── Dockerfile
├── backend/                    Spring Boot — RAG orchestration
│   └── src/main/java/com/cloudrishi/codebasechat/
│       ├── controller/
│       │   └── ChatController.java     REST endpoints — /api/index, /api/chat
│       ├── service/
│       │   └── ChatService.java        RAG pipeline orchestration
│       ├── client/
│       │   └── AiWorkerClient.java     WebClient — calls Python microservice
│       ├── model/
│       │   ├── QueryRequest.java
│       │   ├── QueryResponse.java
│       │   ├── IndexRequest.java
│       │   └── CodeChunk.java
│       └── config/
│           └── OllamaConfig.java       LangChain4j Ollama bean configuration
├── frontend/                   Next.js — chat UI
│   └── app/
│       ├── components/
│       │   ├── ChatPanel.tsx           Question input + answer display
│       │   ├── ChunkCard.tsx           Retrieved source chunk card
│       │   ├── Header.tsx              Branding bar
│       │   └── IndexPanel.tsx          Repo path input + index trigger
│       ├── globals.css                 Design tokens — IBM Plex Mono, #00D4FF accent
│       ├── layout.tsx
│       └── page.tsx
└── README.md
```

---

## environment variables

### ai-worker `.env`

```
DB_HOST=localhost
DB_PORT=5432
DB_NAME=codebase_chat
DB_USER=admin
DB_PASSWORD=admin
```

### backend `application.yml`

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/codebase_chat
    username: admin
    password: admin

ai-worker:
  base-url: http://localhost:8001

ollama:
  base-url: http://localhost:11434
  model: llama3.1:8b
```

---

## future improvements

- **Git webhook trigger** — auto re-index on push to main branch
- **Hybrid search** — combine pgvector cosine similarity with PostgreSQL full-text search for better recall
- **Brace-counting chunker** — find real method end boundaries instead of fixed 50-line limit
- **Token-aware chunking** — enforce 512-token limit per chunk to prevent embedding truncation
- **Graph view** — visualize class/method relationships in the React frontend
- **Multi-repo support** — index and query multiple repositories simultaneously

---

## author

**cloudrishi** — Senior Backend Engineer / Technical Architect  
[github.com/cloudrishi](https://github.com/cloudrishi)
