import type { ReactNode } from "react";

type Tone = "ok" | "off" | "warn" | "info";

const TONE_CLASSES: Record<Tone, string> = {
  ok: "bg-verde-claro text-verde",
  off: "bg-rojo-claro text-rojo",
  warn: "bg-ambar-claro text-ambar",
  info: "bg-azul-claro text-azul-oscuro",
};

export function Badge({ tone, children }: { tone: Tone; children: ReactNode }) {
  return (
    <span
      className={`inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-semibold ${TONE_CLASSES[tone]}`}
    >
      {children}
    </span>
  );
}
