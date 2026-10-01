import { Inbox, type LucideIcon } from "lucide-react";

export function EmptyState({ message, icon: Icon = Inbox }: { message: string; icon?: LucideIcon }) {
  return (
    <div className="flex flex-col items-center gap-2 py-10 text-texto-suave">
      <Icon size={28} strokeWidth={1.5} />
      <p className="text-sm">{message}</p>
    </div>
  );
}
