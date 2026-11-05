import { Mail, MailWarning, Send } from "lucide-react";
import { useState } from "react";
import { extraerMensajeError } from "../../api/client";
import type { Factura } from "../../api/types";
import { useToast } from "../../components/ui/ToastProvider";
import { facturasApi } from "./api";

/**
 * RF-09: a quién se mandó el documento aprobado, o por qué no, con el botón para (re)enviarlo
 * (por ejemplo, después de cargarle el email al cliente).
 */
export function EstadoCorreo({
  documento,
  onActualizado,
}: {
  documento: Pick<Factura, "id" | "estadoDte" | "emailEnviado" | "clienteEmail">;
  onActualizado: (f: Factura) => void;
}) {
  const { showToast } = useToast();
  const [enviando, setEnviando] = useState(false);
  const aprobado = documento.estadoDte === "APROBADO" || documento.estadoDte === "ANULADO";
  if (!aprobado) return null;

  async function enviar() {
    setEnviando(true);
    try {
      const f = await facturasApi.reenviarCorreo(documento.id);
      showToast(`Correo enviado a ${f.clienteEmail}`, "success");
      onActualizado(f);
    } catch (err) {
      showToast(extraerMensajeError(err, "No se pudo enviar el correo"), "error");
    } finally {
      setEnviando(false);
    }
  }

  const boton = (texto: string) => (
    <button
      type="button"
      onClick={enviar}
      disabled={enviando}
      className="inline-flex items-center gap-1 rounded px-1.5 py-0.5 text-xs font-medium text-azul hover:bg-azul-claro disabled:opacity-60"
    >
      <Send size={12} /> {enviando ? "Enviando…" : texto}
    </button>
  );

  if (documento.emailEnviado) {
    return (
      <span className="inline-flex flex-wrap items-center gap-1 text-xs text-texto-suave">
        <Mail size={12} /> Enviada por correo a {documento.clienteEmail ?? "el cliente"}
        {boton("Reenviar")}
      </span>
    );
  }
  if (!documento.clienteEmail) {
    return (
      <span className="inline-flex items-center gap-1 text-xs text-ambar">
        <MailWarning size={12} /> No se envió por correo: el cliente no tiene email (cargalo en Clientes y reenviá)
        {boton("Enviar por correo")}
      </span>
    );
  }
  return (
    <span className="inline-flex items-center gap-1 text-xs text-rojo">
      <MailWarning size={12} /> No se pudo enviar el correo a {documento.clienteEmail}
      {boton("Reintentar")}
    </span>
  );
}
