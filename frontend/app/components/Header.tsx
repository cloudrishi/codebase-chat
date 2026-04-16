/**
 * Header component — branding bar for codebase-chat.
 * Displays the app name and a status indicator.
 */
export default function Header() {
  return (
    <header className="border-b border-[var(--border)] px-6 py-4 flex items-center justify-between">
      <div className="flex items-center gap-3">
        <span className="text-[var(--accent)] font-bold text-lg tracking-tight">
          codebase-chat
        </span>
        <span className="text-[var(--muted)] text-xs">

        </span>
      </div>
      <div className="flex items-center gap-2">
        <span className="w-2 h-2 rounded-full bg-[var(--success)] inline-block" />
        <span className="text-[var(--muted)] text-xs">ollama connected</span>
      </div>
    </header>
  );
}