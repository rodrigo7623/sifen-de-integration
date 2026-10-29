import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { extraerMensajeError } from "../../api/client";
import type { ReporteRechazos } from "../../api/types";
import { Button } from "../../components/ui/Button";
import { EmptyState } from "../../components/ui/EmptyState";
import { Skeleton } from "../../components/ui/Skeleton";
import { reportesApi } from "./api";

const TH = "bg-azul-claro px-3.5 py-2.5 text-left font-semibold text-azul-oscuro";

export function RechazosPage() {
  const navigate = useNavigate();
  const [desde, setDesde] = useState("");
  const [hasta, setHasta] = useState("");
  const [reporte, setReporte] = useState<ReporteRechazos | null>(null);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState<string | null>(null);

  async function cargar(d?: string, h?: string) {
    setCargando(true);
    setError(null);
    try {
      const r = await reportesApi.rechazos(d, h);
      setReporte(r);
      setDesde(r.desde);
      setHasta(r.hasta);
    } catch (err) {
      setError(extraerMensajeError(err, "No se pudo cargar el reporte de rechazos"));
    } finally {
      setCargando(false);
    }
  }

  useEffect(() => {
    cargar();
  }, []);

  const maxIntentosMes = Math.max(1, ...(reporte?.porMes.map((m) => m.intentos) ?? []));

  return (
    <div>
      <h1 className="mb-1 text-xl font-semibold">Rechazos del SIFEN</h1>
      <p className="mb-4 text-sm text-texto-suave">
        Cada intento de envío al SIFEN y su resultado. Una factura rechazada y reenviada cuenta una vez por intento.
      </p>

      <div className="mb-4 flex flex-wrap items-end gap-2">
        <label className="text-sm">
          <span className="mb-1 block text-texto-suave">Desde</span>
          <input type="date" value={desde} onChange={(e) => setDesde(e.target.value)} className="w-auto" />
        </label>
        <label className="text-sm">
          <span className="mb-1 block text-texto-suave">Hasta</span>
          <input type="date" value={hasta} onChange={(e) => setHasta(e.target.value)} className="w-auto" />
        </label>
        <Button variant="secondary" onClick={() => cargar(desde, hasta)}>
          Aplicar
        </Button>
      </div>

      {error && <p className="mb-3 text-sm text-rojo">{error}</p>}

      {cargando || !reporte ? (
        !error && <Skeleton rows={6} />
      ) : (
        <>
          <div className="mb-6 grid grid-cols-2 gap-3 lg:grid-cols-4">
            <Kpi titulo="Intentos de envío" valor={String(reporte.totalIntentos)} />
            <Kpi titulo="Aprobados" valor={String(reporte.aprobados)} tono="text-verde" />
            <Kpi titulo="Rechazados" valor={String(reporte.rechazados)} tono="text-rojo" />
            <Kpi
              titulo="Tasa de aprobación"
              valor={reporte.tasaAprobacion === null ? "—" : `${reporte.tasaAprobacion.toLocaleString("es-PY")} %`}
            />
          </div>

          <div className="mb-6 grid gap-6 lg:grid-cols-2">
            <section>
              <h2 className="mb-2 text-base font-semibold text-azul-oscuro">Rechazos por código</h2>
              <div className="overflow-x-auto rounded-lg bg-white shadow-card">
                <table className="w-full border-collapse text-sm">
                  <thead>
                    <tr>
                      <th className={TH}>Código</th>
                      <th className={TH}>Último mensaje</th>
                      <th className={`${TH} text-right`}>Cantidad</th>
                    </tr>
                  </thead>
                  <tbody>
                    {reporte.porCodigo.length === 0 ? (
                      <tr>
                        <td colSpan={3}>
                          <EmptyState message="Sin rechazos en el período." />
                        </td>
                      </tr>
                    ) : (
                      reporte.porCodigo.map((c) => (
                        <tr key={c.codigo} className="border-b border-borde last:border-0">
                          <td className="px-3.5 py-2.5 font-mono">{c.codigo}</td>
                          <td className="px-3.5 py-2.5 text-texto-suave">{c.mensaje ?? "—"}</td>
                          <td className="px-3.5 py-2.5 text-right font-semibold">{c.cantidad}</td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            </section>

            <section>
              <h2 className="mb-2 text-base font-semibold text-azul-oscuro">Intentos por mes</h2>
              <div className="rounded-lg bg-white p-4 shadow-card">
                <div className="mb-3 flex gap-4 text-xs text-texto-suave">
                  <span className="flex items-center gap-1.5">
                    <span className="inline-block h-2.5 w-2.5 rounded-sm bg-verde" /> Aprobados
                  </span>
                  <span className="flex items-center gap-1.5">
                    <span className="inline-block h-2.5 w-2.5 rounded-sm bg-rojo" /> Rechazados
                  </span>
                </div>
                <div className="flex flex-col gap-2">
                  {reporte.porMes.map((m) => (
                    <div key={m.mes} className="flex items-center gap-3 text-sm">
                      <span className="w-16 shrink-0 tabular-nums text-texto-suave">{m.mes}</span>
                      <div className="flex h-4 flex-1 overflow-hidden rounded bg-azul-claro/50">
                        <div className="bg-verde" style={{ width: `${(m.aprobados / maxIntentosMes) * 100}%` }} />
                        <div className="bg-rojo" style={{ width: `${(m.rechazados / maxIntentosMes) * 100}%` }} />
                      </div>
                      <span className="w-14 shrink-0 text-right tabular-nums">
                        {m.aprobados}/{m.intentos}
                      </span>
                    </div>
                  ))}
                </div>
              </div>
            </section>
          </div>

          <h2 className="mb-2 text-base font-semibold text-azul-oscuro">Últimos rechazos</h2>
          <div className="overflow-x-auto rounded-lg bg-white shadow-card">
            <table className="w-full min-w-[640px] border-collapse text-sm">
              <thead>
                <tr>
                  <th className={TH}>Fecha</th>
                  <th className={TH}>Cliente</th>
                  <th className={TH}>Código</th>
                  <th className={TH}>Mensaje</th>
                </tr>
              </thead>
              <tbody>
                {reporte.ultimosRechazos.length === 0 ? (
                  <tr>
                    <td colSpan={4}>
                      <EmptyState message="Sin rechazos en el período." />
                    </td>
                  </tr>
                ) : (
                  reporte.ultimosRechazos.map((r, i) => (
                    <tr
                      key={`${r.facturaId}-${i}`}
                      className="cursor-pointer border-b border-borde last:border-0 hover:bg-azul-claro/40"
                      onClick={() => navigate(`/facturas/${r.facturaId}`)}
                    >
                      <td className="whitespace-nowrap px-3.5 py-2.5">{new Date(r.fecha).toLocaleString("es-PY")}</td>
                      <td className="px-3.5 py-2.5">{r.clienteRazonSocial}</td>
                      <td className="px-3.5 py-2.5 font-mono">{r.codigo ?? "—"}</td>
                      <td className="px-3.5 py-2.5 text-texto-suave">{r.mensaje ?? "—"}</td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </>
      )}
    </div>
  );
}

function Kpi({ titulo, valor, tono = "text-texto" }: { titulo: string; valor: string; tono?: string }) {
  return (
    <div className="rounded-lg bg-white p-4 shadow-card">
      <div className="text-xs text-texto-suave">{titulo}</div>
      <div className={`text-xl font-semibold ${tono}`}>{valor}</div>
    </div>
  );
}
