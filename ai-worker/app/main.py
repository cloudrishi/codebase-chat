from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from app.parser.java_parser import parse_java_file
from app.embedder import embed_chunks, model
from app.db import store_chunks, search_chunks
import os

app = FastAPI(title="AI Worker - Java Code Chunker")

class IndexRequest(BaseModel):
    repo_path: str

class QueryRequest(BaseModel):
    question: str
    top_k: int = 5

@app.get("/health")
def health():
    return {"status": "ok"}

@app.post("/index")
def index_repository(request: IndexRequest):
    """
    Walks a Java repository, parses every .java file
    into chunks and returns them.
    """
    repo_path = request.repo_path

    if not os.path.exists(repo_path):
        raise HTTPException(status_code=400, detail=f"Path not found: {repo_path}")

    all_chunks = []

    for root, dirs, files in os.walk(repo_path):
        for file in files:
            if file.endswith(".java"):
                file_path = os.path.join(root, file)
                chunks = parse_java_file(file_path)
                all_chunks.extend(chunks)

    enriched = embed_chunks(all_chunks)
    stored = store_chunks(enriched)

    return {
        "total_chunks": stored,
        "status": "indexed successfully"
    }

@app.post("/query")
def query_codebase(request: QueryRequest):
    """
    Takes a natural language question, embeds it,
    and returns the most relevant code chunks from pgvector.
    """
    query_embedding = model.encode(request.question).tolist()
    results = search_chunks(query_embedding, request.top_k)

    if not results:
        raise HTTPException(status_code=404, detail="No relevant chunks found")

    return {
        "question": request.question,
        "results": results
    }