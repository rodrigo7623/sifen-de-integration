import type { ReactNode } from "react";

export function FormField({
  label,
  children,
  error,
}: {
  label: string;
  children: ReactNode;
  error?: string | null;
}) {
  return (
    <div className="mb-3">
      <label className="mb-1 block text-sm font-semibold text-texto-suave">{label}</label>
      {children}
      {error && <p className="mt-1 text-sm text-rojo">{error}</p>}
    </div>
  );
}
