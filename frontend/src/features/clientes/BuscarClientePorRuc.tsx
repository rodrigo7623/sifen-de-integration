import { Search, UserPlus } from "lucide-react";
import { useState } from "react";
import { extraerMensajeError } from "../../api/client";
import { CONDICION_IVA_LABEL, type Cliente, type CondicionIva, type ConsultaRucCliente } from "../../api/types";
import { Button } from "../../components/ui/Button";
import { clientesApi } from "./api";
import { mensajeConsulta, TONO_RUC_CLASE, type TonoRuc } from "./consultaRuc";

/**
 * Carga de factura: busca al cliente por RUC/CI. Si ya está registrado lo selecciona; si no, trae los
 * datos del SIFEN (siConsRUC) y permite registrarlo ahí mismo sin salir de la factura.
 */
export function BuscarClientePorRuc({ onClienteListo }: { onClienteListo: (cliente: Cliente) => void }) {
  const [rucIngresado, setRucIngresado] = useState("");
  const [buscando, setBuscando] = useState(false);
  const [registrando, setRegistrando] = useState(false);
  const [consulta, setConsulta] = useState<ConsultaRucCliente | null>(null);
  const [mensaje, setMensaje] = useState<{ texto: string; tono: TonoRuc } | null>(null);
  const [razonSocial, setRazonSocial] = useState("");
  const [email, setEmail] = useState("");
  const [direccion, setDireccion] = useState("");
  const [condicionIva, setCondicionIva] = useState<CondicionIva>("RESPONSABLE_IVA");

  async function buscar() {
    if (!rucIngresado.trim()) return;
    setBuscando(true);
    setConsulta(null);
    setMensaje(null);
    try {
      const r = await clientesApi.consultarRuc(rucIngresado.trim());
      setRucIngresado(r.ruc);
      if (r.cliente) {
        if (!r.cliente.activo) {
          setMensaje({ texto: `${r.cliente.razonSocial} está desactivado: un administrador debe reactivarlo`, tono: "error" });
          return;
        }
        onClienteListo(r.cliente);
        setMensaje({ texto: `Cliente seleccionado: ${r.cliente.razonSocial}`, tono: "ok" });
        return;
      }
      setConsulta(r);
      setRazonSocial(r.razonSocial ?? "");
      setEmail("");
      setDireccion("");
      setCondicionIva(r.condicionIvaSugerida ?? "RESPONSABLE_IVA");
      setMensaje({ texto: `No es cliente todavía. ${mensajeConsulta(r)}`, tono: r.existeEnSifen ? "ok" : "aviso" });
    } catch (err) {
      setMensaje({ texto: extraerMensajeError(err, "No se pudo consultar el RUC"), tono: "error" });
    } finally {
      setBuscando(false);
    }
  }

  async function registrar() {
    if (!consulta) return;
    if (!razonSocial.trim()) {
      setMensaje({ texto: "Ingresá la razón social o el nombre del cliente", tono: "error" });
      return;
    }
    setRegistrando(true);
    try {
      const cliente = await clientesApi.crear({ ruc: consulta.ruc, razonSocial: razonSocial.trim(), direccion, email, condicionIva });
      onClienteListo(cliente);
      setConsulta(null);
      setMensaje({ texto: `Cliente registrado y seleccionado: ${cliente.razonSocial}`, tono: "ok" });
    } catch (err) {
      setMensaje({ texto: extraerMensajeError(err, "No se pudo registrar el cliente"), tono: "error" });
    } finally {
      setRegistrando(false);
    }
  }

  const etiqueta = "mb-1 block text-sm font-semibold text-texto-suave";

  return (
    <div className="mb-3">
      <label className={etiqueta}>Buscar por RUC / CI</label>
      <div className="flex max-w-md gap-2">
        <input
          value={rucIngresado}
          placeholder="80012345 o 80012345-6"
          onChange={(e) => setRucIngresado(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter") {
              e.preventDefault();
              buscar();
            }
          }}
        />
        <Button type="button" variant="secondary" icon={<Search size={16} />} onClick={buscar} disabled={buscando}>
          {buscando ? "Buscando…" : "Buscar"}
        </Button>
      </div>
      {mensaje && <p className={`mt-1 text-sm ${TONO_RUC_CLASE[mensaje.tono]}`}>{mensaje.texto}</p>}

      {consulta && (
        <div className="mt-3 rounded-lg border border-borde bg-azul-claro/40 p-4">
          <p className="m-0 mb-3 text-sm font-semibold text-azul-oscuro">Registrar cliente {consulta.ruc}</p>
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <div className="sm:col-span-2">
              <label className={etiqueta}>Razón social / nombre</label>
              <input value={razonSocial} onChange={(e) => setRazonSocial(e.target.value)} />
            </div>
            <div>
              <label className={etiqueta}>Email (para enviarle la factura)</label>
              <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} />
            </div>
            <div>
              <label className={etiqueta}>Dirección</label>
              <input value={direccion} onChange={(e) => setDireccion(e.target.value)} />
            </div>
            <div>
              <label className={etiqueta}>Condición ante el IVA</label>
              <select value={condicionIva} onChange={(e) => setCondicionIva(e.target.value as CondicionIva)}>
                {Object.entries(CONDICION_IVA_LABEL).map(([valor, texto]) => (
                  <option key={valor} value={valor}>
                    {texto}
                  </option>
                ))}
              </select>
            </div>
          </div>
          <div className="mt-3 flex justify-end gap-2">
            <Button type="button" variant="secondary" onClick={() => setConsulta(null)}>
              Cancelar
            </Button>
            <Button type="button" icon={<UserPlus size={16} />} onClick={registrar} disabled={registrando}>
              {registrando ? "Registrando…" : "Registrar y usar en la factura"}
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}
