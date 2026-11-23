import { Ban, Hash } from "lucide-react";
import { useState } from "react";
import { extraerMensajeError } from "../../api/client";
import type { EstadoDte, TipoDocumento } from "../../api/types";
import { useAuth } from "../../auth/AuthContext";
import { Button } from "../../components/ui/Button";
import { FormField } from "../../components/ui/FormField";
import { Modal } from "../../components/ui/Modal";
import { useToast } from "../../components/ui/ToastProvider";
import { facturasApi } from "./api";

/** Manual Técnico v150, 11.6.1 (4009/4010): 48 h para cancelar una factura, 168 h para las notas. */
const HORAS_CANCELACION = { FE: 48, NC: 168, ND: 168 } as const;

type Accion = "cancelar" | "inutilizar";

/**
 * Eventos del emisor (solo ADMIN): cancelar en el SIFEN un documento aprobado, dentro del plazo, o
 * inutilizar el número de uno rechazado que no se va a volver a enviar.
 */
export function EventosSifenAcciones({
  documento,
  onActualizado,
}: {
  documento: { id: string; estadoDte: EstadoDte | null; tipoDocumento: TipoDocumento | null; fechaEmision: string | null; numero: string | null };
  onActualizado: () => void | Promise<void>;
}) {
  const { usuario } = useAuth();
  const { showToast } = useToast();
  const [accion, setAccion] = useState<Accion | null>(null);
  const [motivo, setMotivo] = useState("");
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (usuario?.rol !== "ADMIN") return null;
  const horas = HORAS_CANCELACION[documento.tipoDocumento ?? "FE"];
  const vence = documento.fechaEmision ? new Date(documento.fechaEmision).getTime() + horas * 3600_000 : 0;
  const puedeCancelar = documento.estadoDte === "APROBADO" && vence > Date.now();
  const puedeInutilizar = documento.estadoDte === "RECHAZADO" && !!documento.numero;
  if (!puedeCancelar && !puedeInutilizar) return null;

  function abrir(a: Accion) {
    setAccion(a);
    setMotivo("");
    setError(null);
  }

  async function enviar() {
    if (!accion) return;
    if (motivo.trim().length < 5) {
      setError("El motivo debe tener al menos 5 caracteres");
      return;
    }
    setEnviando(true);
    setError(null);
    try {
      const evento = accion === "cancelar"
        ? await facturasApi.cancelar(documento.id, motivo.trim())
        : await facturasApi.inutilizar(documento.id, motivo.trim());
      if (evento.estado === "APROBADO") {
        showToast(accion === "cancelar" ? "Documento cancelado en el SIFEN" : "Número inutilizado en el SIFEN", "success");
        setAccion(null);
        await onActualizado();
      } else {
        setError(`El SIFEN rechazó el evento: ${evento.codigo ?? ""} ${evento.mensaje ?? ""}`.trim());
      }
    } catch (err) {
      setError(extraerMensajeError(err, "No se pudo enviar el evento al SIFEN"));
    } finally {
      setEnviando(false);
    }
  }

  return (
    <>
      <div className="mb-4 flex flex-wrap gap-2">
        {puedeCancelar && (
          <Button variant="danger" icon={<Ban size={16} />} onClick={() => abrir("cancelar")}>
            Cancelar en el SIFEN
          </Button>
        )}
        {puedeInutilizar && (
          <Button variant="secondary" icon={<Hash size={16} />} onClick={() => abrir("inutilizar")}>
            Inutilizar número
          </Button>
        )}
      </div>
      {accion && (
        <Modal
          title={accion === "cancelar" ? `Cancelar ${documento.numero ?? "documento"}` : `Inutilizar ${documento.numero ?? ""}`}
          onClose={() => setAccion(null)}
          footer={
            <>
              <Button variant="secondary" onClick={() => setAccion(null)} disabled={enviando}>Volver</Button>
              <Button variant="danger" onClick={enviar} disabled={enviando}>
                {enviando ? "Enviando al SIFEN…" : accion === "cancelar" ? "Cancelar documento" : "Inutilizar número"}
              </Button>
            </>
          }
        >
          <p className="mb-3 text-sm text-texto-suave">
            {accion === "cancelar"
              ? `El documento queda sin efecto en el SIFEN y no se puede deshacer. Plazo: ${horas} h desde la emisión.`
              : "El SIFEN rechazó este documento. Si no lo vas a corregir y reenviar, inutilizá su número para que no quede como un salto en la numeración. No se puede deshacer."}
          </p>
          <FormField label="Motivo" error={error}>
            <textarea className="w-full" rows={3} maxLength={500} value={motivo} onChange={(e) => setMotivo(e.target.value)} />
          </FormField>
        </Modal>
      )}
    </>
  );
}
