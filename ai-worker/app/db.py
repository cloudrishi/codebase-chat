import psycopg2
from pgvector.psycopg2 import register_vector
from dotenv import load_dotenv
import os

load_dotenv()

def get_connection():
    conn = psycopg2.connect(
        host=os.getenv("DB_HOST", "localhost"),
        port=os.getenv("DB_PORT", 5432),
        dbname=os.getenv("DB_NAME", "codebase_chat"),
        user=os.getenv("DB_USER", "admin"),
        password=os.getenv("DB_PASSWORD", "admin")
    )
    register_vector(conn)
    return conn

def store_chunks(enriched_chunks: list[dict]):
    """
    Takes the enriched chunks from embedder.py
    and inserts them into pgvector.
    if any insert fails, the existing data is preserved.
    """
    if not enriched_chunks:
        return 0
    
    conn = get_connection()
    cursor = conn.cursor()

    try:
        # Scoped delete by file_path instead of TRUNCATE
        # preserves data from other repos and is transaction-safe
        file_paths = list({chunk["file_path"] for chunk in enriched_chunks})
        cursor.execute(
            "DELETE FROM codebase.code_chunks WHERE file_path = ANY(%s)",
            (file_paths,)
        )

        for chunk in enriched_chunks:
            cursor.execute("""
                INSERT INTO codebase.code_chunks 
                    (file_path, class_name, method_name, chunk_type, content, embedding)
                VALUES 
                    (%s, %s, %s, %s, %s, %s)
            """, (
                chunk["file_path"],
                chunk["class_name"],
                chunk["method_name"],
                chunk["chunk_type"],
                chunk["content"],
                chunk["embedding"]
            ))

        conn.commit()
        return len(enriched_chunks)
    except Exception as e:
        conn.rollback()
        raise RuntimeError(f"Failed to store chunks: {e}")
    finally:
        cursor.close()
        conn.close()

def search_chunks(query_embedding: list[float], top_k: int = 5) -> list[dict]:
    """
    Takes a query embedding and finds the most semantically
    similar chunks in pgvector using cosine similarity.
    """
    conn = get_connection()
    cursor = conn.cursor()

    try:
        cursor.execute("""
            SELECT 
                file_path,
                class_name,
                method_name,
                chunk_type,
                content,
                1 - (embedding <=> %s::vector) AS similarity
            FROM codebase.code_chunks
            ORDER BY embedding <=> %s::vector
            LIMIT %s
        """, (query_embedding, query_embedding, top_k))

        rows = cursor.fetchall()

        return [
            {
                "file_path": row[0],
                "class_name": row[1],
                "method_name": row[2],
                "chunk_type": row[3],
                "content": row[4],
                "similarity": round(float(row[5]), 4)
            }
            for row in rows
        ]

    except Exception as e:
        raise RuntimeError(f"Failed to search chunks: {e}")

    finally:
        cursor.close()
        conn.close()