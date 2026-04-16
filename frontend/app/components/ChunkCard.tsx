interface Chunk {
  filePath: string;
  className: string;
  methodName: string | null;
  chunkType: string;
  content: string;
  similarity: number;
}

interface ChunkCardProps {
  chunk: Chunk;
  index: number;
}

/**
 * ChunkCard — displays a single retrieved code chunk
 * with its metadata, similarity score, and source code content.
 */
export default function ChunkCard({ chunk, index }: ChunkCardProps) {
  const similarityPercent = Math.round(chunk.similarity * 100);
  const fileName = chunk.filePath.split("/").pop();

  return (
    <div className="border border-[var(--border)] bg-[var(--surface)] mb-3">
      {/* Header */}
      <div className="flex items-center justify-between px-3 py-2 
                      border-b border-[var(--border)]">
        <div className="flex items-center gap-2">
          <span className="text-[var(--accent)] text-xs">
            [{index + 1}]
          </span>
          <span className="text-[var(--muted)] text-xs">
            {chunk.chunkType}
          </span>
          <span className="text-[var(--foreground)] text-xs font-bold">
            {chunk.className}
            {chunk.methodName ? `.${chunk.methodName}()` : ""}
          </span>
        </div>
        <div className="flex items-center gap-3">
          <span className="text-[var(--muted)] text-xs">
            {fileName}
          </span>
          <span className={`text-xs ${
            similarityPercent > 35
              ? "text-[var(--success)]"
              : "text-[var(--warning)]"
          }`}>
            {similarityPercent}% match
          </span>
        </div>
      </div>

      {/* Code content */}
      <pre className="px-3 py-3 text-xs text-[var(--foreground)] 
                      overflow-x-auto whitespace-pre-wrap leading-relaxed">
        {chunk.content}
      </pre>
    </div>
  );
}