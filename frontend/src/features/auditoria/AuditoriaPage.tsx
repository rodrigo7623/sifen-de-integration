import { ChevronLeft, ChevronRight } from "lucide-react";
import { useEffect, useState } from "react";
import { extraerMensajeError } from "../../api/client";
import type { AuditoriaEntrada, PaginaSpring } from "../../api/types";
import { EmptyState } from "../../components/ui/EmptyState";
import { Skeleton } from "../../components/ui/Skeleton";
import { auditoriaApi } from "./api";

export function AuditoriaPage() {
  const [pagina, setPagina] = useState<PaginaSpring<AuditoriaEntrada> | null>(null);
  const [numeroPagina, setNumeroPagina] = useState(0);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setCargando(true);
    auditoriaApi
      .listar(numeroPagina)
      .then(setPagina)
      .catch((err) => setError(extraerMensajeError(err, "No se pudo cargar la auditoría")))
      .finally(() => setCargando(false));
  }, [numeroPagina]);

  return (
    <div>
      <h1 className="mb-4 text-xl font-semibold">Auditoría</h1>
      <p className="mb-4 text-sm text-texto-suave">
        Historial de operaciones sobre facturas (RF-08): quién hizo qué y cuándo.
      </p>

      {error && <p className="mb-3 text-sm text-rojo">{error}</p>}

      <div className="overflow-x-auto rounded-lg bg-white shadow-card">
        <table className="w-full min-w-[560px] border-collapse text-sm">
          <thead>
            <tr>
              <th className="bg-azul-claro px-3.5 py-2.5 text-left font-semibold text-azul-oscuro">
                Fecha y hora
              </th>
              <th className="bg-azul-claro px-3.5 py-2.5 text-left font-semibold text-azul-oscuro">
                Operación
              </th>
              <th className="bg-azul-claro px-3.5 py-2.5 text-left font-semibold text-azul-oscuro">
                Usuario
              </th>
              <th className="bg-azul-claro px-3.5 py-2.5 text-left font-semibold text-azul-oscuro">
                Factura
              </th>
            </tr>
          </thead>
          <tbody>
            {cargando ? (
              <tr>
                <td colSpan={4} className="p-3">
                  <Skeleton rows={5} />
                </td>
              </tr>
            ) : !pagina || pagina.content.length === 0 ? (
              <tr>
                <td colSpan={4}>
                  <EmptyState message="No hay registros de auditoría todavía." />
                </td>
              </tr>
            ) : (
              pagina.content.map((entrada) => (
                <tr key={entrada.id} className="border-b border-borde last:border-0">
                  <td className="px-3.5 py-2.5">{new Date(entrada.fechaHora).toLocaleString("es-PY")}</td>
                  <td className="px-3.5 py-2.5 font-medium">{entrada.operacion}</td>
                  <td className="px-3.5 py-2.5">{entrada.usuarioNombre ?? "—"}</td>
                  <td className="px-3.5 py-2.5 text-texto-suave">
                    {entrada.facturaId ? entrada.facturaId.slice(0, 8) + "…" : "—"}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>

        {pagina && pagina.totalPages > 1 && (
          <div className="flex items-center justify-between border-t border-borde px-3.5 py-2.5 text-sm text-texto-suave">
            <span>
              Página {pagina.number + 1} de {pagina.totalPages} · {pagina.totalElements} registros
            </span>
            <div className="flex gap-1">
              <button
                className="rounded-md p-1 hover:bg-azul-claro disabled:opacity-40"
                disabled={numeroPagina === 0}
                onClick={() => setNumeroPagina((p) => p - 1)}
                aria-label="Página anterior"
              >
                <ChevronLeft size={18} />
              </button>
              <button
                className="rounded-md p-1 hover:bg-azul-claro disabled:opacity-40"
                disabled={numeroPagina >= pagina.totalPages - 1}
                onClick={() => setNumeroPagina((p) => p + 1)}
                aria-label="Página siguiente"
              >
                <ChevronRight size={18} />
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
