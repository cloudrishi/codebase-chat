from sentence_transformers import SentenceTransformer
from app.parser.java_parser import CodeChunk

model = SentenceTransformer('all-MiniLM-L6-v2')

def embed_chunks(chunks: list[CodeChunk]) -> list[dict]:
    """
    Takes a list of CodeChunks and returns them enriched
    with their embedding vectors.
    """
    contents = [chunk.content for chunk in chunks]
    embeddings = model.encode(contents, show_progress_bar=True)

    results = []
    for chunk, embedding in zip(chunks, embeddings):
        results.append({
            "file_path": chunk.file_path,
            "class_name": chunk.class_name,
            "method_name": chunk.method_name,
            "chunk_type": chunk.chunk_type,
            "content": chunk.content,
            "embedding": embedding.tolist()
        })

    return results