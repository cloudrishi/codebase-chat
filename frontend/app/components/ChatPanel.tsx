"use client";

import { useState } from "react";
import ChunkCard from "./ChunkCard";

interface Chunk {
  filePath: string;
  className: string;
  methodName: string | null;
  chunkType: string;
  content: string;
  similarity: number;
}

interface ChatResponse {
  question: string;
  answer: string;
  relevantChunks: Chunk[];
}

/**
 * ChatPanel — main chat interface for querying the indexed codebase.
 * Handles question input, API communication, and renders
 * the AI answer alongside retrieved source chunks.
 */
export default function ChatPanel() {
  const [question, setQuestion] = useState("");
  const [topK, setTopK] = useState(5);
  const [response, setResponse] = useState<ChatResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const handleChat = async () => {
    if (!question.trim()) return;
    setLoading(true);
    setError("");
    setResponse(null);

    try {
      const res = await fetch(`${process.env.NEXT_PUBLIC_API_URL}/api/chat`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ question, topK }),
      });

      const data: ChatResponse = await res.json();
      setResponse(data);
    } catch (err) {
      setError("Failed to connect to backend");
    } finally {
      setLoading(false);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      handleChat();
    }
  };

  return (
    <div className="flex-1 flex flex-col px-6 py-4 overflow-y-auto">

      {/* Input area */}
      <div className="mb-6">
        <p className="text-[var(--muted)] text-xs mb-2">// ask a question about your codebase</p>
        <div className="flex gap-2 mb-2">
          <textarea
            value={question}
            onChange={(e) => setQuestion(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="how does authentication work?"
            rows={3}
            className="flex-1 bg-[var(--surface)] border border-[var(--border)]
                       text-[var(--foreground)] px-3 py-2 text-xs outline-none
                       focus:border-[var(--accent)] transition-colors resize-none"
          />
          <div className="flex flex-col gap-2">
            <button
              onClick={handleChat}
              disabled={loading}
              className="px-4 py-2 text-xs border border-[var(--accent)]
                         text-[var(--accent)] hover:bg-[var(--accent)]
                         hover:text-[#0a0a0a] transition-colors disabled:opacity-50"
            >
              {loading ? "thinking..." : "ask →"}
            </button>
            <div className="flex items-center gap-2">
              <span className="text-[var(--muted)] text-xs">top</span>
              <input
                type="number"
                value={topK}
                onChange={(e) => setTopK(Number(e.target.value))}
                min={1}
                max={10}
                className="w-12 bg-[var(--surface)] border border-[var(--border)]
                           text-[var(--foreground)] px-2 py-1 text-xs outline-none
                           focus:border-[var(--accent)] transition-colors"
              />
            </div>
          </div>
        </div>
        <p className="text-[var(--muted)] text-xs">
          press enter to submit — shift+enter for new line
        </p>
      </div>

      {/* Error */}
      {error && (
        <p className="text-red-400 text-xs mb-4">✗ {error}</p>
      )}

      {/* Loading */}
      {loading && (
        <div className="text-[var(--accent)] text-xs mb-4 animate-pulse">
          ▸ querying codebase...
        </div>
      )}

      {/* Response */}
      {response && (
        <div className="flex-1">

          {/* Answer */}
          <div className="border border-[var(--accent)] bg-[var(--surface)] p-4 mb-6">
            <p className="text-[var(--accent)] text-xs mb-3">
              ▸ answer
            </p>
            <p className="text-[var(--foreground)] text-xs leading-relaxed whitespace-pre-wrap">
              {response.answer}
            </p>
          </div>

          {/* Chunks */}
          <div>
            <p className="text-[var(--muted)] text-xs mb-3">
              
            </p>
            {response.relevantChunks.map((chunk, i) => (
              <ChunkCard key={i} chunk={chunk} index={i} />
            ))}
          </div>
        </div>
      )}
    </div>
  );
}