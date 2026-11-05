import { SearchCheck } from "lucide-react";
import { useState } from "react";
import { extraerMensajeError } from "../../api/client";
import type { Factura } from "../../api/types";
import { Button } from "../../components/ui/Button";
import { useToast } from "../../components/ui/ToastProvider";
import { facturasApi } from "./api";

/** Mensaje del toast según cómo quedó un documento después de enviarlo o verificarlo en el SIFEN. */
export function resultadoEnvio(f: Pick<Factura, "estadoDte">, documento: string): [string, "success" | "error" | "info"] {
  if (f.estadoDte === "APROBADO") return [`${documento} aprobada por el SIFEN`, "success"];
  if (f.estadoDte === "EN_PROCESO") return ["El SIFEN no respondió: verificá el estado del documento", "info"];
  return [`${documento} rechazada por el SIFEN`, "error"];
}

/**
 * Documento EN_PROCESO: se envió pero el SIFEN no respondió, así que pudo haber quedado aprobado.
 * No se reabre (generaría otro CDC); se consulta por su CDC y, si el SIFEN no lo tiene, se reenvía
 * el mismo documento.
 */
/** Manual Técnico v150, 6.2: el DE se transmite dentro de las 72 h siguientes a su firma. */
const PLAZO_TRANSMISION_MS = 72 * 60 * 60 * 1000;

export function VerificarEnvioAviso({
  documento,
  motivo,
  firmadoEl,
  onVerificado,
}: {
  documento: Pick<Factura, "id">;
  motivo: string | null;
  /** Fecha de firma (la de emisión, se fijan juntas al confirmar). */
  firmadoEl?: string | null;
  onVerificado: (f: Factura) => void;
}) {
  const vence = firmadoEl ? new Date(new Date(firmadoEl).getTime() + PLAZO_TRANSMISION_MS) : null;
  const vencido = vence !== null && vence.getTime() < Date.now();
  const { showToast } = useToast();
  const [verificando, setVerificando] = useState(false);

  async function verificar() {
    setVerificando(true);
    try {
      const f = await facturasApi.verificar(documento.id);
      const [mensaje, tipo] = resultadoEnvio(f, "Documento");
      showToast(mensaje, tipo);
      onVerificado(f);
    } catch (err) {
      showToast(extraerMensajeError(err, "No se pudo verificar el documento en el SIFEN"), "error");
    } finally {
      setVerificando(false);
    }
  }

  return (
    <div className="mb-4 rounded-lg border border-ambar bg-ambar-claro p-5">
      <p className="mb-1 text-sm font-semibold text-ambar">Envío sin confirmar</p>
      <p className="mb-3 text-sm">
        El SIFEN no respondió al envío, así que el documento pudo haber quedado aprobado. Verificalo antes de
        hacer cualquier otra cosa: si el SIFEN no lo tiene, se reenvía con el mismo número y CDC.
        {motivo && <span className="mt-1 block text-xs text-texto-suave">{motivo}</span>}
      </p>
      {vence && (
        <p className={`mb-3 text-sm ${vencido ? "font-semibold text-rojo" : ""}`}>
          {vencido
            ? "Pasaron las 72 h para transmitirlo (Manual Técnico 6.2): si el SIFEN no lo tiene, no se reenvía, queda rechazado y hay que reabrirlo para firmarlo de nuevo."
            : `Plazo para transmitirlo: hasta el ${vence.toLocaleString("es-PY", { dateStyle: "short", timeStyle: "short" })} (72 h desde la firma).`}
        </p>
      )}
      <Button variant="secondary" icon={<SearchCheck size={16} />} onClick={verificar} disabled={verificando}>
        {verificando ? "Consultando al SIFEN…" : "Verificar en el SIFEN"}
      </Button>
    </div>
  );
}
