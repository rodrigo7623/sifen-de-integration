import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { extraerMensajeError } from "../../api/client";
import { ESTADO_DTE_LABEL, type EstadoDte, type HistorialCliente } from "../../api/types";
import { Badge } from "../../components/ui/Badge";
import { EmptyState } from "../../components/ui/EmptyState";
import { Modal } from "../../components/ui/Modal";
import { Skeleton } from "../../components/ui/Skeleton";
import { guaranies, reportesApi } from "./api";

const TH = "bg-azul-claro px-3 py-2 text-left font-semibold text-azul-oscuro";

function tono(estado: EstadoDte): "ok" | "off" | "info" {
  if (estado === "APROBADO") return "ok";
  if (estado === "RECHAZADO") return "off";
  return "info";
}

/** Historial de facturación de un cliente (Release 3): resumen y cada factura con sus intentos de envío. */
export function HistorialClienteModal({ ruc, onClose }: { ruc: string; onClose: () => void }) {
  const navigate = useNavigate();
  const [historial, setHistorial] = useState<HistorialCliente | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    reportesApi
      .historialCliente(ruc)
      .then(setHistorial)
      .catch((err) => setError(extraerMensajeError(err, "No se pudo cargar el historial")));
  }, [ruc]);

  return (
    <Modal title={historial ? `Historial de ${historial.razonSocial}` : "Historial del cliente"} onClose={onClose} maxWidthClassName="max-w-4xl">
      {error ? (
        <p className="text-sm text-rojo">{error}</p>
      ) : !historial ? (
        <Skeleton rows={4} />
      ) : (
        <>
          <div className="mb-4 grid grid-cols-2 gap-3 text-sm sm:grid-cols-4">
            <Dato titulo="Facturas" valor={String(historial.cantidadFacturas)} />
            <Dato titulo="Aprobadas / rechazadas" valor={`${historial.aprobadas} / ${historial.rechazadas}`} />
            <Dato titulo="Monto aprobado" valor={`Gs. ${guaranies(historial.montoAprobado)}`} />
            <Dato titulo="Envíos rechazados" valor={`${historial.intentosRechazados} de ${historial.intentosEnvio}`} />
          </div>

          <div className="overflow-x-auto rounded-lg border border-borde">
            <table className="w-full min-w-[640px] border-collapse text-sm">
              <thead>
                <tr>
                  <th className={TH}>Fecha</th>
                  <th className={TH}>Número</th>
                  <th className={TH}>Estado</th>
                  <th className={`${TH} text-right`}>Total</th>
                  <th className={`${TH} text-right`}>Envíos</th>
                  <th className={TH}>Última respuesta del SIFEN</th>
                </tr>
              </thead>
              <tbody>
                {historial.facturas.length === 0 ? (
                  <tr>
                    <td colSpan={6}>
                      <EmptyState message="El cliente todavía no tiene facturas." />
                    </td>
                  </tr>
                ) : (
                  historial.facturas.map((f) => (
                    <tr
                      key={f.facturaId}
                      className="cursor-pointer border-b border-borde last:border-0 hover:bg-azul-claro/40"
                      onClick={() => navigate(`/facturas/${f.facturaId}`)}
                    >
                      <td className="whitespace-nowrap px-3 py-2">{new Date(f.fechaEmision).toLocaleDateString("es-PY")}</td>
                      <td className="whitespace-nowrap px-3 py-2">{f.numero}</td>
                      <td className="px-3 py-2">
                        <Badge tone={tono(f.estado)}>{ESTADO_DTE_LABEL[f.estado]}</Badge>
                      </td>
                      <td className="px-3 py-2 text-right tabular-nums">{guaranies(f.total)}</td>
                      <td className="px-3 py-2 text-right">{f.intentos}</td>
                      <td className="px-3 py-2 text-texto-suave">
                        {f.ultimoCodigo ? `${f.ultimoCodigo} · ${f.ultimoMensaje ?? ""}` : "—"}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </>
      )}
    </Modal>
  );
}

function Dato({ titulo, valor }: { titulo: string; valor: string }) {
  return (
    <div className="rounded-lg bg-azul-claro/40 p-3">
      <div className="text-xs text-texto-suave">{titulo}</div>
      <div className="font-semibold">{valor}</div>
    </div>
  );
}
