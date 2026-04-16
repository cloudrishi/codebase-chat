"use client";

import { useState } from "react";

interface IndexPanelProps {
  onIndexed: () => void;
}

/**
 * IndexPanel — allows the user to specify a Java repository path
 * and trigger the indexing pipeline via the Spring Boot backend.
 */
export default function IndexPanel({ onIndexed }: IndexPanelProps) {
  const [repoPath, setRepoPath] = useState("/Users/rish/working/p2-projects/master-data-service/src/main/java");
  const [status, setStatus] = useState<"idle" | "indexing" | "done" | "error">("idle");
  const [message, setMessage] = useState("");

  const handleIndex = async () => {
    if (!repoPath.trim()) return;
    setStatus("indexing");
    setMessage("");

    try {
      const res = await fetch("http://localhost:8080/api/index", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ repoPath }),
      });

      const text = await res.text();
      setStatus("done");
      setMessage(text);
      onIndexed();
    } catch (err) {
      setStatus("error");
      setMessage("Failed to connect to backend");
    }
  };

  return (
    <div className="border-b border-[var(--border)] px-6 py-4">
      <p className="text-[var(--muted)] text-xs mb-2">{/*index a java repository */}

      </p>
      <div className="flex gap-2">
        <input
          type="text"
          value={repoPath}
          onChange={(e) => setRepoPath(e.target.value)}
          placeholder="/Users/rish/working/p2-projects/master-data-service/src/main/java"
          className="flex-1 bg-[var(--surface)] border border-[var(--border)] 
                     text-[var(--foreground)] px-3 py-2 text-xs outline-none
                     focus:border-[var(--accent)] transition-colors"
        />
        <button
          onClick={handleIndex}
          disabled={status === "indexing"}
          className="px-4 py-2 text-xs border border-[var(--accent)] 
                     text-[var(--accent)] hover:bg-[var(--accent)] 
                     hover:text-[#0a0a0a] transition-colors disabled:opacity-50"
        >
          {status === "indexing" ? "indexing..." : "index"}
        </button>
      </div>
      {message && (
        <p className={`text-xs mt-2 ${
          status === "error" 
            ? "text-red-400" 
            : "text-[var(--success)]"
        }`}>
          {status === "done" ? "✓ " : "✗ "}{message}
        </p>
      )}
    </div>
  );
}