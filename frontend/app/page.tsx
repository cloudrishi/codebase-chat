"use client";

import { useState } from "react";
import ChatPanel from "./components/ChatPanel";
import Header from "./components/Header";
import IndexPanel from "./components/IndexPanel";

/**
 * Main page — composes the codebase-chat UI.
 * Manages the indexed state to conditionally show the chat panel.
 */
export default function Home() {
  const [indexed, setIndexed] = useState(false);

  return (
    <div className="h-screen flex flex-col overflow-hidden">
      <Header />
      <IndexPanel onIndexed={() => setIndexed(true)} />
      {indexed ? (
        <ChatPanel />
      ) : (
        <div className="flex-1 flex items-center justify-center">
          <div className="text-center">
            <p className="text-[var(--accent)] text-sm mb-2">
              ▸ index a repository to get started
            </p>
            <p className="text-[var(--muted)] text-xs">
              {/* point to a java project and click index */}
            </p>
          </div>
        </div>
      )}
    </div>
  );
}