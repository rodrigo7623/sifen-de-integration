import { FileDown, FileSpreadsheet } from "lucide-react";
import { useEffect, useState } from "react";
import { extraerMensajeError } from "../../api/client";
import type { LibroIva } from "../../api/types";
import { Button } from "../../components/ui/Button";
import { EmptyState } from "../../components/ui/EmptyState";
import { Skeleton } from "../../components/ui/Skeleton";
import { useToast } from "../../components/ui/ToastProvider";
import { descargarArchivo } from "../facturas/api";
import { guaranies, reportesApi } from "./api";

const TH = "bg-azul-claro px-3 py-2.5 text-left font-semibold text-azul-oscuro whitespace-nowrap";
const TH_NUM = `${TH} text-right`;
const TD = "px-3 py-2";
const TD_NUM = "px-3 py-2 text-right tabular-nums";

function mesActual(): string {
  const hoy = new Date();
  return `${hoy.getFullYear()}-${String(hoy.getMonth() + 1).padStart(2, "0")}`;
}

/** FE, NC o ND: la factura y sus notas comparten la numeración visible, así que el tipo las distingue. */
function siglaComprobante(tipo: string) {
  if (tipo.startsWith("Nota de crédito")) return "NC";
  if (tipo.startsWith("Nota de débito")) return "ND";
  return "FE";
}

export function LibroIvaPage() {
  const { showToast } = useToast();
  const [periodo, setPeriodo] = useState(mesActual());
  const [libro, setLibro] = useState<LibroIva | null>(null);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [descargando, setDescargando] = useState<"excel" | "pdf" | null>(null);

  useEffect(() => {
    if (!periodo) return;
    setCargando(true);
    setError(null);
    reportesApi
      .libroIva(periodo)
      .then(setLibro)
      .catch((err) => setError(extraerMensajeError(err, "No se pudo cargar el Libro IVA")))
      .finally(() => setCargando(false));
  }, [periodo]);

  async function descargar(formato: "excel" | "pdf") {
    setDescargando(formato);
    try {
      const blob = await reportesApi.libroIvaArchivo(periodo, formato);
      descargarArchivo(blob, `libro-iva-ventas-${periodo}.${formato === "excel" ? "xlsx" : "pdf"}`);
    } catch {
      showToast("No se pudo descargar el archivo", "error");
    } finally {
      setDescargando(null);
    }
  }

  const sinDatos = !libro || libro.renglones.length === 0;

  return (
    <div>
      <h1 className="mb-1 text-xl font-semibold">Libro IVA Ventas</h1>
      <p className="mb-4 text-sm text-texto-suave">
        Comprobantes aprobados por el SIFEN en el mes, con los montos separados por tasa. Los montos gravados
        incluyen el IVA.
      </p>

      <div className="mb-4 flex flex-wrap items-end gap-2">
        <label className="text-sm">
          <span className="mb-1 block text-texto-suave">Período</span>
          <input type="month" value={periodo} onChange={(e) => setPeriodo(e.target.value)} className="w-auto" />
        </label>
        <div className="flex gap-2 sm:ml-auto">
          <Button
            variant="secondary"
            icon={<FileSpreadsheet size={16} />}
            disabled={sinDatos || descargando !== null}
            onClick={() => descargar("excel")}
          >
            {descargando === "excel" ? "Generando…" : "Excel"}
          </Button>
          <Button
            variant="secondary"
            icon={<FileDown size={16} />}
            disabled={sinDatos || descargando !== null}
            onClick={() => descargar("pdf")}
          >
            {descargando === "pdf" ? "Generando…" : "PDF"}
          </Button>
        </div>
      </div>

      {error && <p className="mb-3 text-sm text-rojo">{error}</p>}

      {libro && !sinDatos && (
        <div className="mb-4 grid grid-cols-2 gap-3 lg:grid-cols-4">
          <Resumen titulo="Comprobantes" valor={String(libro.totales.cantidadComprobantes)} />
          <Resumen titulo="Total vendido" valor={`Gs. ${guaranies(libro.totales.total)}`} />
          <Resumen titulo="Base imponible 10% / 5%" valor={`${guaranies(libro.totales.baseImponible10)} / ${guaranies(libro.totales.baseImponible5)}`} />
          <Resumen titulo="IVA débito fiscal" valor={`Gs. ${guaranies(libro.totales.totalIva)}`} destacado />
        </div>
      )}

      <div className="overflow-x-auto rounded-lg bg-white shadow-card">
        <table className="w-full min-w-[960px] border-collapse text-sm">
          <thead>
            <tr>
              <th className={TH}>Fecha</th>
              <th className={TH}>Tipo</th>
              <th className={TH}>Número</th>
              <th className={TH}>RUC/CI</th>
              <th className={TH}>Razón social</th>
              <th className={TH}>Condición</th>
              <th className={TH_NUM}>Gravado 10%</th>
              <th className={TH_NUM}>IVA 10%</th>
              <th className={TH_NUM}>Gravado 5%</th>
              <th className={TH_NUM}>IVA 5%</th>
              <th className={TH_NUM}>Exento</th>
              <th className={TH_NUM}>Total</th>
            </tr>
          </thead>
          <tbody>
            {cargando ? (
              <tr>
                <td colSpan={12} className="p-3">
                  <Skeleton rows={5} />
                </td>
              </tr>
            ) : sinDatos ? (
              <tr>
                <td colSpan={12}>
                  <EmptyState message="No hay comprobantes aprobados en este período." />
                </td>
              </tr>
            ) : (
              <>
                {libro.renglones.map((r) => (
                  <tr key={r.facturaId} className="border-b border-borde">
                    <td className={TD}>{new Date(`${r.fecha}T00:00:00`).toLocaleDateString("es-PY")}</td>
                    <td className={TD} title={r.tipoComprobante}>
                      {siglaComprobante(r.tipoComprobante)}
                    </td>
                    <td className={`${TD} whitespace-nowrap`}>{r.numero}</td>
                    <td className={TD}>{r.clienteRuc}</td>
                    <td className={TD}>{r.clienteRazonSocial}</td>
                    <td className={TD}>{r.condicion}</td>
                    <td className={TD_NUM}>{guaranies(r.gravado10)}</td>
                    <td className={TD_NUM}>{guaranies(r.iva10)}</td>
                    <td className={TD_NUM}>{guaranies(r.gravado5)}</td>
                    <td className={TD_NUM}>{guaranies(r.iva5)}</td>
                    <td className={TD_NUM}>{guaranies(r.exento)}</td>
                    <td className={TD_NUM}>{guaranies(r.total)}</td>
                  </tr>
                ))}
                <tr className="bg-azul-claro/40 font-semibold">
                  <td className={TD} colSpan={6}>
                    Totales
                  </td>
                  <td className={TD_NUM}>{guaranies(libro.totales.gravado10)}</td>
                  <td className={TD_NUM}>{guaranies(libro.totales.iva10)}</td>
                  <td className={TD_NUM}>{guaranies(libro.totales.gravado5)}</td>
                  <td className={TD_NUM}>{guaranies(libro.totales.iva5)}</td>
                  <td className={TD_NUM}>{guaranies(libro.totales.exento)}</td>
                  <td className={TD_NUM}>{guaranies(libro.totales.total)}</td>
                </tr>
              </>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function Resumen({ titulo, valor, destacado = false }: { titulo: string; valor: string; destacado?: boolean }) {
  return (
    <div className="rounded-lg bg-white p-4 shadow-card">
      <div className="text-xs text-texto-suave">{titulo}</div>
      <div className={`text-lg font-semibold ${destacado ? "text-azul-oscuro" : "text-texto"}`}>{valor}</div>
    </div>
  );
}
