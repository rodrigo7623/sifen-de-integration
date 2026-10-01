import { ChevronLeft, ChevronRight } from "lucide-react";
import { useEffect, useState, type ReactNode } from "react";
import { EmptyState } from "./EmptyState";
import { Skeleton } from "./Skeleton";

export interface Column<T> {
  header: string;
  render: (row: T) => ReactNode;
  className?: string;
}

interface DataTableProps<T> {
  columns: Column<T>[];
  data: T[];
  keyExtractor: (row: T) => string;
  loading?: boolean;
  emptyMessage?: string;
  pageSize?: number;
}

/** Tabla genérica con paginación client-side (el volumen de datos de este proyecto no justifica
 * todavía paginación en el backend). */
export function DataTable<T>({
  columns,
  data,
  keyExtractor,
  loading = false,
  emptyMessage = "No hay datos para mostrar.",
  pageSize = 10,
}: DataTableProps<T>) {
  const [pagina, setPagina] = useState(0);

  useEffect(() => {
    setPagina(0);
  }, [data]);

  const totalPaginas = Math.max(1, Math.ceil(data.length / pageSize));
  const datosPagina = data.slice(pagina * pageSize, pagina * pageSize + pageSize);

  return (
    <div className="overflow-x-auto rounded-lg bg-white shadow-card">
      <table className="w-full min-w-[640px] border-collapse text-sm">
        <thead>
          <tr>
            {columns.map((col) => (
              <th
                key={col.header}
                className="bg-azul-claro px-3.5 py-2.5 text-left font-semibold text-azul-oscuro"
              >
                {col.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {loading ? (
            <tr>
              <td colSpan={columns.length} className="p-3">
                <Skeleton rows={3} />
              </td>
            </tr>
          ) : datosPagina.length === 0 ? (
            <tr>
              <td colSpan={columns.length}>
                <EmptyState message={emptyMessage} />
              </td>
            </tr>
          ) : (
            datosPagina.map((row) => (
              <tr key={keyExtractor(row)} className="border-b border-borde last:border-0">
                {columns.map((col) => (
                  <td key={col.header} className={`px-3.5 py-2.5 ${col.className ?? ""}`}>
                    {col.render(row)}
                  </td>
                ))}
              </tr>
            ))
          )}
        </tbody>
      </table>

      {!loading && data.length > pageSize && (
        <div className="flex items-center justify-between border-t border-borde px-3.5 py-2.5 text-sm text-texto-suave">
          <span>
            Página {pagina + 1} de {totalPaginas} · {data.length} resultados
          </span>
          <div className="flex gap-1">
            <button
              className="rounded-md p-1 hover:bg-azul-claro disabled:opacity-40"
              disabled={pagina === 0}
              onClick={() => setPagina((p) => p - 1)}
              aria-label="Página anterior"
            >
              <ChevronLeft size={18} />
            </button>
            <button
              className="rounded-md p-1 hover:bg-azul-claro disabled:opacity-40"
              disabled={pagina >= totalPaginas - 1}
              onClick={() => setPagina((p) => p + 1)}
              aria-label="Página siguiente"
            >
              <ChevronRight size={18} />
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
