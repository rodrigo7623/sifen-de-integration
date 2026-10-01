import { Download, FileCode2, Mail, Plus, RotateCcw, X } from "lucide-react";
import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { extraerMensajeError } from "../../api/client";
import {
  CONDICION_PAGO_LABEL,
  ESTADO_DTE_LABEL,
  TASA_IVA_LABEL,
  type CondicionPago,
  type EstadoDte,
  type TasaIva,
} from "../../api/types";
import { clientesApi } from "../clientes/api";
import type { Cliente, Establecimiento } from "../../api/types";
import { productosApi } from "../catalogo/api";
import type { Producto } from "../../api/types";
import { establecimientosApi } from "../establecimientos/api";
import { Badge } from "../../components/ui/Badge";
import { Button } from "../../components/ui/Button";
import { useConfirm } from "../../components/ui/ConfirmProvider";
import { useToast } from "../../components/ui/ToastProvider";
import { descargarArchivo, facturasApi, type FacturaInput, type ItemFacturaInput } from "./api";
import { calcularTotales, subtotalItem } from "./totales";

interface FilaItem extends ItemFacturaInput {
  localId: string;
}

function nuevaFilaVacia(): FilaItem {
  return {
    localId: crypto.randomUUID(),
    productoCodigo: null,
    descripcion: "",
    cantidad: 1,
    precioUnitario: 0,
    tasaIva: "DIEZ",
  };
}

function badgeTone(estado: EstadoDte): "ok" | "off" | "info" {
  if (estado === "APROBADO") return "ok";
  if (estado === "RECHAZADO") return "off";
  return "info";
}

export function FacturaFormPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const confirmar = useConfirm();
  const { showToast } = useToast();

  const [clientes, setClientes] = useState<Cliente[]>([]);
  const [productos, setProductos] = useState<Producto[]>([]);
  const [establecimientos, setEstablecimientos] = useState<Establecimiento[]>([]);
  const [productoParaAgregar, setProductoParaAgregar] = useState("");

  const [clienteRuc, setClienteRuc] = useState("");
  const [establecimientoId, setEstablecimientoId] = useState("");
  const [puntoExpedicionId, setPuntoExpedicionId] = useState("");
  const [condicionPago, setCondicionPago] = useState<CondicionPago>("CONTADO");
  const [plazoDias, setPlazoDias] = useState<number | "">("");
  const [cantidadCuotas, setCantidadCuotas] = useState<number | "">("");
  const [items, setItems] = useState<FilaItem[]>([]);

  const [facturaId, setFacturaId] = useState<string | null>(null);
  const [estadoDte, setEstadoDte] = useState<EstadoDte | null>(null);
  const [clienteRazonSocial, setClienteRazonSocial] = useState<string | null>(null);
  const [establecimientoTexto, setEstablecimientoTexto] = useState<string | null>(null);
  const [puntoExpedicionTexto, setPuntoExpedicionTexto] = useState<string | null>(null);
  const [motivoRechazo, setMotivoRechazo] = useState<string | null>(null);
  const [emailEnviado, setEmailEnviado] = useState(false);

  const [cargando, setCargando] = useState(false);
  const [guardando, setGuardando] = useState(false);
  const [confirmando, setConfirmando] = useState(false);
  const [reabriendo, setReabriendo] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const esBorrador = estadoDte === null || estadoDte === "BORRADOR";
  const esRechazada = estadoDte === "RECHAZADO";
  const puedeDescargar = estadoDte === "APROBADO" || estadoDte === "RECHAZADO";

  useEffect(() => {
    (async () => {
      const [clientesData, productosData, establecimientosData] = await Promise.all([
        clientesApi.buscar(),
        productosApi.buscar(),
        establecimientosApi.listar(),
      ]);
      setClientes(clientesData);
      setProductos(productosData);
      setEstablecimientos(establecimientosData);
      // Para una factura nueva, preseleccionar el primer establecimiento/punto activo.
      if (!id && establecimientosData.length > 0) {
        setEstablecimientoId(establecimientosData[0].id);
        const primerPunto = establecimientosData[0].puntosExpedicion.find((p) => p.activo);
        if (primerPunto) setPuntoExpedicionId(primerPunto.id);
      }
    })().catch((err) => setError(extraerMensajeError(err, "No se pudo cargar catálogo/clientes")));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function onCambiarEstablecimiento(nuevoId: string) {
    setEstablecimientoId(nuevoId);
    const establecimiento = establecimientos.find((e) => e.id === nuevoId);
    const primerPunto = establecimiento?.puntosExpedicion.find((p) => p.activo);
    setPuntoExpedicionId(primerPunto?.id ?? "");
  }

  function cargarDesdeFactura(factura: Awaited<ReturnType<typeof facturasApi.obtener>>) {
    setFacturaId(factura.id);
    setEstadoDte(factura.estadoDte);
    setClienteRuc(factura.clienteRuc);
    setClienteRazonSocial(factura.clienteRazonSocial);
    setEstablecimientoId(factura.establecimientoId);
    setPuntoExpedicionId(factura.puntoExpedicionId);
    setEstablecimientoTexto(`${factura.establecimientoCodigo} — ${factura.establecimientoDenominacion}`);
    setPuntoExpedicionTexto(
      `${factura.puntoExpedicionCodigo}${factura.puntoExpedicionDescripcion ? " — " + factura.puntoExpedicionDescripcion : ""}`,
    );
    setCondicionPago(factura.condicionPago);
    setPlazoDias(factura.plazoDias ?? "");
    setCantidadCuotas(factura.cantidadCuotas ?? "");
    setMotivoRechazo(factura.motivoRechazo);
    setEmailEnviado(factura.emailEnviado);
    setItems(
      factura.items.map((item) => ({
        localId: crypto.randomUUID(),
        productoCodigo: item.productoCodigo,
        descripcion: item.descripcion,
        cantidad: item.cantidad,
        precioUnitario: item.precioUnitario,
        tasaIva: item.tasaIva,
      })),
    );
  }

  useEffect(() => {
    if (!id) return;
    setCargando(true);
    facturasApi
      .obtener(id)
      .then(cargarDesdeFactura)
      .catch((err) => setError(extraerMensajeError(err, "No se pudo cargar la factura")))
      .finally(() => setCargando(false));
  }, [id]);

  function agregarDesdeCatalogo() {
    const producto = productos.find((p) => p.codigo === productoParaAgregar);
    if (!producto) return;
    setItems((prev) => [
      ...prev,
      {
        localId: crypto.randomUUID(),
        productoCodigo: producto.codigo,
        descripcion: producto.descripcion,
        cantidad: 1,
        precioUnitario: producto.precioBase,
        tasaIva: producto.tasaIva,
      },
    ]);
    setProductoParaAgregar("");
  }

  function agregarAdHoc() {
    setItems((prev) => [...prev, nuevaFilaVacia()]);
  }

  function quitarItem(localId: string) {
    setItems((prev) => prev.filter((i) => i.localId !== localId));
  }

  function actualizarItem(localId: string, cambios: Partial<FilaItem>) {
    setItems((prev) => prev.map((i) => (i.localId === localId ? { ...i, ...cambios } : i)));
  }

  function armarInput(): FacturaInput {
    return {
      clienteRuc,
      establecimientoId,
      puntoExpedicionId,
      condicionPago,
      plazoDias: condicionPago === "CREDITO" && plazoDias !== "" ? Number(plazoDias) : null,
      cantidadCuotas: condicionPago === "CREDITO" && cantidadCuotas !== "" ? Number(cantidadCuotas) : null,
      items: items.map(({ localId: _localId, ...resto }) => resto),
    };
  }

  async function guardarBorrador() {
    setError(null);
    if (!clienteRuc) {
      setError("Seleccioná un cliente");
      return;
    }
    if (!establecimientoId || !puntoExpedicionId) {
      setError("Seleccioná el establecimiento y el punto de expedición");
      return;
    }
    if (items.length === 0) {
      setError("Agregá al menos un ítem");
      return;
    }
    setGuardando(true);
    try {
      const input = armarInput();
      const factura = facturaId ? await facturasApi.editar(facturaId, input) : await facturasApi.crear(input);
      setFacturaId(factura.id);
      setEstadoDte(factura.estadoDte);
      setClienteRazonSocial(factura.clienteRazonSocial);
      showToast("Borrador guardado", "success");
      if (!id) {
        navigate(`/facturas/${factura.id}`, { replace: true });
      }
    } catch (err) {
      setError(extraerMensajeError(err, "No se pudo guardar el borrador"));
    } finally {
      setGuardando(false);
    }
  }

  async function confirmarEnvio() {
    if (!facturaId) return;
    if (!(await confirmar("¿Confirmar y enviar esta factura al SIFEN? Ya no se podrá editar."))) return;
    setError(null);
    setConfirmando(true);
    try {
      const factura = await facturasApi.confirmar(facturaId);
      setEstadoDte(factura.estadoDte);
      setMotivoRechazo(factura.motivoRechazo);
      setEmailEnviado(factura.emailEnviado);
      showToast(
        factura.estadoDte === "APROBADO" ? "Factura aprobada por el SIFEN" : "Factura rechazada por el SIFEN",
        factura.estadoDte === "APROBADO" ? "success" : "error",
      );
    } catch (err) {
      setError(extraerMensajeError(err, "No se pudo confirmar el envío"));
    } finally {
      setConfirmando(false);
    }
  }

  async function reabrir() {
    if (!facturaId) return;
    if (!(await confirmar("¿Reabrir esta factura rechazada para corregirla?"))) return;
    setError(null);
    setReabriendo(true);
    try {
      const factura = await facturasApi.reabrir(facturaId);
      setEstadoDte(factura.estadoDte);
      setMotivoRechazo(factura.motivoRechazo);
    } catch (err) {
      setError(extraerMensajeError(err, "No se pudo reabrir la factura"));
    } finally {
      setReabriendo(false);
    }
  }

  async function descargar(tipo: "pdf" | "xml") {
    if (!facturaId) return;
    try {
      const blob = tipo === "pdf" ? await facturasApi.descargarPdf(facturaId) : await facturasApi.descargarXml(facturaId);
      descargarArchivo(blob, `factura-${facturaId}.${tipo}`);
    } catch (err) {
      showToast(extraerMensajeError(err, `No se pudo descargar el ${tipo.toUpperCase()}`), "error");
    }
  }

  const totales = calcularTotales(items);

  if (cargando) return <p className="text-texto-suave">Cargando…</p>;

  const sectionClass = "mb-4 rounded-lg border border-borde bg-white p-5 pt-4 disabled:opacity-75";

  return (
    <div>
      <div className="mb-4 flex items-center justify-between">
        <h1 className="m-0 text-xl font-semibold">{id ? "Factura" : "Nueva factura manual"}</h1>
        <Button variant="secondary" onClick={() => navigate("/facturas")}>
          Volver
        </Button>
      </div>

      {estadoDte && (
        <p className="mb-3 flex items-center gap-2 text-sm">
          Estado: <Badge tone={badgeTone(estadoDte)}>{ESTADO_DTE_LABEL[estadoDte]}</Badge>
          {emailEnviado && (
            <span className="inline-flex items-center gap-1 text-xs text-texto-suave">
              <Mail size={12} /> Enviada por correo al cliente
            </span>
          )}
        </p>
      )}

      {esRechazada && motivoRechazo && (
        <div className="mb-4 rounded-lg border border-rojo bg-rojo-claro/40 p-5">
          <p className="mb-3 text-sm text-rojo">Motivo del rechazo: {motivoRechazo}</p>
          <Button variant="danger" icon={<RotateCcw size={16} />} onClick={reabrir} disabled={reabriendo}>
            {reabriendo ? "Reabriendo…" : "Reabrir para corregir"}
          </Button>
        </div>
      )}

      {puedeDescargar && (
        <div className="mb-4 flex gap-2">
          <Button variant="secondary" icon={<Download size={16} />} onClick={() => descargar("pdf")}>
            Descargar PDF
          </Button>
          <Button variant="secondary" icon={<FileCode2 size={16} />} onClick={() => descargar("xml")}>
            Descargar XML
          </Button>
        </div>
      )}

      {error && <p className="mb-3 text-sm text-rojo">{error}</p>}

      <fieldset disabled={!esBorrador} className={sectionClass}>
        <legend className="px-1.5 font-semibold text-azul-oscuro">1. Datos del cliente y punto de emisión</legend>
        <label className="mb-1 block text-sm font-semibold text-texto-suave">Cliente</label>
        <select value={clienteRuc} onChange={(e) => setClienteRuc(e.target.value)}>
          <option value="">Seleccionar cliente…</option>
          {clientes.map((c) => (
            <option key={c.ruc} value={c.ruc}>
              {c.razonSocial} — {c.ruc}
            </option>
          ))}
        </select>
        {!esBorrador && clienteRazonSocial && (
          <p className="mt-1 text-sm text-texto-suave">{clienteRazonSocial}</p>
        )}

        <div className="mt-3 flex flex-wrap gap-4">
          <div className="min-w-[220px] flex-1">
            <label className="mb-1 block text-sm font-semibold text-texto-suave">Establecimiento</label>
            <select value={establecimientoId} onChange={(e) => onCambiarEstablecimiento(e.target.value)}>
              <option value="">Seleccionar establecimiento…</option>
              {establecimientos.map((est) => (
                <option key={est.id} value={est.id}>
                  {est.codigo} — {est.denominacion}
                </option>
              ))}
            </select>
            {!esBorrador && establecimientoTexto && (
              <p className="mt-1 text-sm text-texto-suave">{establecimientoTexto}</p>
            )}
          </div>
          <div className="min-w-[220px] flex-1">
            <label className="mb-1 block text-sm font-semibold text-texto-suave">Punto de expedición</label>
            <select value={puntoExpedicionId} onChange={(e) => setPuntoExpedicionId(e.target.value)}>
              <option value="">Seleccionar punto de expedición…</option>
              {establecimientos
                .find((est) => est.id === establecimientoId)
                ?.puntosExpedicion.filter((p) => p.activo)
                .map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.codigo}
                    {p.descripcion ? ` — ${p.descripcion}` : ""}
                  </option>
                ))}
            </select>
            {!esBorrador && puntoExpedicionTexto && (
              <p className="mt-1 text-sm text-texto-suave">{puntoExpedicionTexto}</p>
            )}
          </div>
        </div>
      </fieldset>

      <fieldset disabled={!esBorrador} className={sectionClass}>
        <legend className="px-1.5 font-semibold text-azul-oscuro">2. Ítems de la factura</legend>

        {esBorrador && (
          <div className="mb-3 flex flex-wrap items-center gap-2">
            <select
              className="max-w-xs"
              value={productoParaAgregar}
              onChange={(e) => setProductoParaAgregar(e.target.value)}
            >
              <option value="">Elegir producto del catálogo…</option>
              {productos.map((p) => (
                <option key={p.codigo} value={p.codigo}>
                  {p.descripcion} ({p.codigo})
                </option>
              ))}
            </select>
            <Button
              type="button"
              variant="secondary"
              onClick={agregarDesdeCatalogo}
              disabled={!productoParaAgregar}
            >
              Agregar desde catálogo
            </Button>
            <Button type="button" variant="secondary" icon={<Plus size={16} />} onClick={agregarAdHoc}>
              Ítem manual
            </Button>
          </div>
        )}

        <div className="overflow-x-auto rounded-lg shadow-card">
          <table className="w-full min-w-[640px] border-collapse text-sm">
            <thead>
              <tr>
                <th className="bg-azul-claro px-3.5 py-2.5 text-left font-semibold text-azul-oscuro">
                  Descripción
                </th>
                <th className="bg-azul-claro px-3.5 py-2.5 text-left font-semibold text-azul-oscuro">
                  Cantidad
                </th>
                <th className="bg-azul-claro px-3.5 py-2.5 text-left font-semibold text-azul-oscuro">
                  Precio unit.
                </th>
                <th className="bg-azul-claro px-3.5 py-2.5 text-left font-semibold text-azul-oscuro">IVA</th>
                <th className="bg-azul-claro px-3.5 py-2.5 text-left font-semibold text-azul-oscuro">
                  Subtotal
                </th>
                {esBorrador && <th className="bg-azul-claro px-3.5 py-2.5"></th>}
              </tr>
            </thead>
            <tbody>
              {items.map((item) => (
                <tr key={item.localId} className="border-b border-borde last:border-0">
                  <td className="px-3.5 py-2.5">
                    <input
                      value={item.descripcion}
                      onChange={(e) => actualizarItem(item.localId, { descripcion: e.target.value })}
                    />
                  </td>
                  <td className="px-3.5 py-2.5">
                    <input
                      type="number"
                      min={1}
                      value={item.cantidad}
                      onChange={(e) => actualizarItem(item.localId, { cantidad: Number(e.target.value) })}
                    />
                  </td>
                  <td className="px-3.5 py-2.5">
                    <input
                      type="number"
                      min={0}
                      value={item.precioUnitario}
                      onChange={(e) =>
                        actualizarItem(item.localId, { precioUnitario: Number(e.target.value) })
                      }
                    />
                  </td>
                  <td className="px-3.5 py-2.5">
                    <select
                      value={item.tasaIva}
                      onChange={(e) => actualizarItem(item.localId, { tasaIva: e.target.value as TasaIva })}
                    >
                      {Object.entries(TASA_IVA_LABEL).map(([valor, etiqueta]) => (
                        <option key={valor} value={valor}>
                          {etiqueta}
                        </option>
                      ))}
                    </select>
                  </td>
                  <td className="px-3.5 py-2.5">{subtotalItem(item).toLocaleString("es-PY")}</td>
                  {esBorrador && (
                    <td className="px-3.5 py-2.5">
                      <button
                        type="button"
                        className="rounded-md p-1 text-rojo hover:bg-rojo-claro"
                        onClick={() => quitarItem(item.localId)}
                        aria-label="Quitar ítem"
                      >
                        <X size={16} />
                      </button>
                    </td>
                  )}
                </tr>
              ))}
              {items.length === 0 && (
                <tr>
                  <td colSpan={6} className="py-6 text-center text-sm text-texto-suave">
                    Sin ítems todavía.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </fieldset>

      <fieldset disabled={!esBorrador} className={sectionClass}>
        <legend className="px-1.5 font-semibold text-azul-oscuro">3. Condición de pago</legend>
        <div className="flex gap-4">
          <label className="flex items-center gap-1.5 text-sm">
            <input
              type="radio"
              className="w-auto"
              name="condicionPago"
              checked={condicionPago === "CONTADO"}
              onChange={() => setCondicionPago("CONTADO")}
            />
            {CONDICION_PAGO_LABEL.CONTADO}
          </label>
          <label className="flex items-center gap-1.5 text-sm">
            <input
              type="radio"
              className="w-auto"
              name="condicionPago"
              checked={condicionPago === "CREDITO"}
              onChange={() => setCondicionPago("CREDITO")}
            />
            {CONDICION_PAGO_LABEL.CREDITO}
          </label>
        </div>

        {condicionPago === "CREDITO" && (
          <div className="mt-3 flex gap-4">
            <div>
              <label className="mb-1 block text-sm font-semibold text-texto-suave">Plazo (días)</label>
              <input
                type="number"
                min={1}
                value={plazoDias}
                onChange={(e) => setPlazoDias(e.target.value === "" ? "" : Number(e.target.value))}
              />
            </div>
            <div>
              <label className="mb-1 block text-sm font-semibold text-texto-suave">Cuotas</label>
              <input
                type="number"
                min={1}
                value={cantidadCuotas}
                onChange={(e) => setCantidadCuotas(e.target.value === "" ? "" : Number(e.target.value))}
              />
            </div>
          </div>
        )}
      </fieldset>

      <fieldset className={sectionClass}>
        <legend className="px-1.5 font-semibold text-azul-oscuro">4. Totales</legend>
        <table className="w-full text-sm">
          <tbody>
            <tr>
              <td className="py-1">IVA 5%</td>
              <td className="py-1">{totales.totalIva5.toLocaleString("es-PY")}</td>
            </tr>
            <tr>
              <td className="py-1">IVA 10%</td>
              <td className="py-1">{totales.totalIva10.toLocaleString("es-PY")}</td>
            </tr>
            <tr>
              <td className="py-1 font-bold">Total general</td>
              <td className="py-1 font-bold">{totales.totalGeneral.toLocaleString("es-PY")}</td>
            </tr>
          </tbody>
        </table>
      </fieldset>

      <div className="flex justify-end gap-2">
        {esBorrador && (
          <Button variant="secondary" onClick={guardarBorrador} disabled={guardando}>
            {guardando ? "Guardando…" : "Guardar borrador"}
          </Button>
        )}
        {facturaId && esBorrador && (
          <Button onClick={confirmarEnvio} disabled={confirmando}>
            {confirmando ? "Enviando…" : "Confirmar y enviar al SIFEN"}
          </Button>
        )}
      </div>
    </div>
  );
}
