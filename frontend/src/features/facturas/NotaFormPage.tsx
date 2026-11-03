import { Download, FileCode2, Mail, Plus, RotateCcw, X } from "lucide-react";
import { useEffect, useState } from "react";
import { useNavigate, useParams, useSearchParams } from "react-router-dom";
import { extraerMensajeError } from "../../api/client";
import {
  ESTADO_DTE_LABEL,
  MOTIVO_NOTA_LABEL,
  TASA_IVA_LABEL,
  TIPO_DOCUMENTO_LABEL,
  type EstadoDte,
  type Factura,
  type MotivoNota,
  type TasaIva,
  type TipoDocumento,
} from "../../api/types";
import { Badge } from "../../components/ui/Badge";
import { Button } from "../../components/ui/Button";
import { useConfirm } from "../../components/ui/ConfirmProvider";
import { useToast } from "../../components/ui/ToastProvider";
import { descargarArchivo, facturasApi, nombreArchivo, type ItemFacturaInput } from "./api";
import { calcularTotales, subtotalItem } from "./totales";

interface FilaItem extends ItemFacturaInput {
  localId: string;
}

function badgeTone(estado: EstadoDte): "ok" | "off" | "warn" | "info" {
  if (estado === "APROBADO") return "ok";
  if (estado === "RECHAZADO") return "off";
  if (estado === "ANULADO") return "warn";
  return "info";
}

function filaDesde(item: ItemFacturaInput): FilaItem {
  const { productoCodigo, descripcion, cantidad, precioUnitario, tasaIva } = item;
  return { productoCodigo, descripcion, cantidad, precioUnitario, tasaIva, localId: crypto.randomUUID() };
}

/**
 * M6: nota de crédito/débito sobre una factura aprobada. Cliente, establecimiento y punto de
 * expedición salen de la factura; acá solo se eligen el tipo, el motivo (E401) y los ítems. Una vez
 * guardada, se confirma y envía al SIFEN igual que una factura.
 */
export function NotaFormPage() {
  const { id } = useParams<{ id: string }>();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const confirmar = useConfirm();
  const { showToast } = useToast();

  const [factura, setFactura] = useState<Factura | null>(null);
  const [otrasNotas, setOtrasNotas] = useState<Factura[]>([]);
  const [nota, setNota] = useState<Factura | null>(null);

  const [tipo, setTipo] = useState<TipoDocumento>("NC");
  const [motivo, setMotivo] = useState<MotivoNota>("DEVOLUCION");
  const [items, setItems] = useState<FilaItem[]>([]);
  const [itemParaCopiar, setItemParaCopiar] = useState("");

  const [cargando, setCargando] = useState(true);
  const [guardando, setGuardando] = useState(false);
  const [confirmando, setConfirmando] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const estadoDte = nota?.estadoDte ?? null;
  const esBorrador = estadoDte === null || estadoDte === "BORRADOR";
  const puedeDescargar = estadoDte === "APROBADO" || estadoDte === "RECHAZADO";

  async function cargarFactura(facturaId: string, notaId: string | null) {
    const [f, notas] = await Promise.all([facturasApi.obtener(facturaId), facturasApi.notas(facturaId)]);
    setFactura(f);
    setOtrasNotas(notas.filter((n) => n.id !== notaId));
    return f;
  }

  useEffect(() => {
    (async () => {
      setCargando(true);
      setError(null);
      try {
        if (id) {
          const n = await facturasApi.obtener(id);
          setNota(n);
          setTipo(n.tipoDocumento);
          setMotivo(n.motivoEmision ?? "DEVOLUCION");
          setItems(n.items.map((i) => filaDesde(i)));
          if (n.facturaAsociadaId) await cargarFactura(n.facturaAsociadaId, n.id);
        } else {
          const facturaId = searchParams.get("factura");
          if (!facturaId) {
            setError("Falta indicar la factura a la que se le emite la nota");
            return;
          }
          const f = await cargarFactura(facturaId, null);
          // Por defecto, devolución total: los mismos ítems de la factura, editables.
          setItems(f.items.map((i) => filaDesde(i)));
        }
      } catch (err) {
        setError(extraerMensajeError(err, "No se pudo cargar la nota"));
      } finally {
        setCargando(false);
      }
    })();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  function actualizarItem(localId: string, cambios: Partial<FilaItem>) {
    setItems((prev) => prev.map((i) => (i.localId === localId ? { ...i, ...cambios } : i)));
  }

  function copiarItemDeLaFactura() {
    const original = factura?.items[Number(itemParaCopiar)];
    if (!original) return;
    setItems((prev) => [...prev, filaDesde(original)]);
    setItemParaCopiar("");
  }

  function agregarManual() {
    setItems((prev) => [
      ...prev,
      filaDesde({ productoCodigo: null, descripcion: "", cantidad: 1, precioUnitario: 0, tasaIva: "DIEZ" }),
    ]);
  }

  const totales = calcularTotales(items);
  // Rechazo 2417: las notas de crédito (no rechazadas) no pueden superar el total de la factura.
  const acreditado = otrasNotas
    .filter((n) => n.tipoDocumento === "NC" && n.estadoDte !== "RECHAZADO")
    .reduce((s, n) => s + n.totalGeneral, 0);
  const saldoAcreditable = factura ? factura.totalGeneral - acreditado : 0;
  const superaSaldo = tipo === "NC" && totales.totalGeneral > saldoAcreditable;

  async function guardar() {
    if (!factura) return;
    setError(null);
    if (items.length === 0) {
      setError("Agregá al menos un ítem");
      return;
    }
    if (superaSaldo) {
      setError("El total de la nota de crédito supera el saldo acreditable de la factura");
      return;
    }
    setGuardando(true);
    try {
      const input = {
        facturaAsociadaId: factura.id,
        tipo,
        motivo,
        items: items.map(({ localId: _localId, ...resto }) => resto),
      };
      const guardada = nota ? await facturasApi.editarNota(nota.id, input) : await facturasApi.crearNota(input);
      setNota(guardada);
      showToast("Borrador de la nota guardado", "success");
      if (!id) navigate(`/notas/${guardada.id}`, { replace: true });
    } catch (err) {
      setError(extraerMensajeError(err, "No se pudo guardar la nota"));
    } finally {
      setGuardando(false);
    }
  }

  async function confirmarEnvio() {
    if (!nota) return;
    const etiqueta = TIPO_DOCUMENTO_LABEL[nota.tipoDocumento].toLowerCase();
    if (!(await confirmar(`¿Confirmar y enviar esta ${etiqueta} al SIFEN? Ya no se podrá editar.`))) return;
    setError(null);
    setConfirmando(true);
    try {
      const enviada = await facturasApi.confirmar(nota.id);
      setNota(enviada);
      showToast(
        enviada.estadoDte === "APROBADO" ? "Nota aprobada por el SIFEN" : "Nota rechazada por el SIFEN",
        enviada.estadoDte === "APROBADO" ? "success" : "error",
      );
      if (enviada.facturaAsociadaId) await cargarFactura(enviada.facturaAsociadaId, enviada.id);
    } catch (err) {
      setError(extraerMensajeError(err, "No se pudo confirmar el envío"));
    } finally {
      setConfirmando(false);
    }
  }

  async function reabrir() {
    if (!nota) return;
    if (!(await confirmar("¿Reabrir esta nota rechazada para corregirla?"))) return;
    try {
      setNota(await facturasApi.reabrir(nota.id));
    } catch (err) {
      setError(extraerMensajeError(err, "No se pudo reabrir la nota"));
    }
  }

  async function descargar(extension: "pdf" | "xml") {
    if (!nota) return;
    try {
      const blob = extension === "pdf" ? await facturasApi.descargarPdf(nota.id) : await facturasApi.descargarXml(nota.id);
      descargarArchivo(blob, nombreArchivo(nota, extension));
    } catch (err) {
      showToast(extraerMensajeError(err, `No se pudo descargar el ${extension.toUpperCase()}`), "error");
    }
  }

  if (cargando) return <p className="text-texto-suave">Cargando…</p>;

  const sectionClass = "mb-4 rounded-lg border border-borde bg-white p-5 pt-4";
  const th = "bg-azul-claro px-3.5 py-2.5 text-left font-semibold text-azul-oscuro";
  const titulo = nota?.numero
    ? `${TIPO_DOCUMENTO_LABEL[tipo]} N° ${nota.numero}`
    : nota
      ? `${TIPO_DOCUMENTO_LABEL[tipo]} (borrador)`
      : "Nueva nota de crédito / débito";

  return (
    <div>
      <div className="mb-4 flex items-center justify-between">
        <h1 className="m-0 text-xl font-semibold">{titulo}</h1>
        <Button variant="secondary" onClick={() => navigate(factura ? `/facturas/${factura.id}` : "/facturas")}>
          Volver
        </Button>
      </div>

      {nota && estadoDte && (
        <p className="mb-3 flex flex-wrap items-center gap-2 text-sm">
          Estado: <Badge tone={badgeTone(estadoDte)}>{ESTADO_DTE_LABEL[estadoDte]}</Badge>
          {nota.cdc && <span className="font-mono text-xs text-texto-suave">CDC {nota.cdc}</span>}
          {nota.emailEnviado && (
            <span className="inline-flex items-center gap-1 text-xs text-texto-suave">
              <Mail size={12} /> Enviada por correo al cliente
            </span>
          )}
        </p>
      )}

      {estadoDte === "RECHAZADO" && nota?.motivoRechazo && (
        <div className="mb-4 rounded-lg border border-rojo bg-rojo-claro/40 p-5">
          <p className="mb-3 text-sm text-rojo">Motivo del rechazo: {nota.motivoRechazo}</p>
          <Button variant="danger" icon={<RotateCcw size={16} />} onClick={reabrir}>
            Reabrir para corregir
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

      {factura && (
        <section className={sectionClass}>
          <h2 className="m-0 mb-2 text-base font-semibold text-azul-oscuro">Factura asociada</h2>
          <dl className="grid grid-cols-1 gap-x-6 gap-y-1 text-sm sm:grid-cols-2">
            <div>
              <dt className="inline font-semibold text-texto-suave">Número: </dt>
              <dd className="inline">
                <button
                  type="button"
                  className="text-azul underline-offset-2 hover:underline"
                  onClick={() => navigate(`/facturas/${factura.id}`)}
                >
                  {factura.numero}
                </button>{" "}
                <Badge tone={badgeTone(factura.estadoDte)}>{ESTADO_DTE_LABEL[factura.estadoDte]}</Badge>
              </dd>
            </div>
            <div>
              <dt className="inline font-semibold text-texto-suave">Cliente: </dt>
              <dd className="inline">
                {factura.clienteRazonSocial} — {factura.clienteRuc}
              </dd>
            </div>
            <div>
              <dt className="inline font-semibold text-texto-suave">Total: </dt>
              <dd className="inline">{factura.totalGeneral.toLocaleString("es-PY")}</dd>
            </div>
            <div>
              <dt className="inline font-semibold text-texto-suave">Saldo acreditable: </dt>
              <dd className="inline">{saldoAcreditable.toLocaleString("es-PY")}</dd>
            </div>
            <div className="sm:col-span-2">
              <dt className="inline font-semibold text-texto-suave">CDC: </dt>
              <dd className="inline break-all font-mono text-xs">{factura.cdc ?? "—"}</dd>
            </div>
          </dl>
        </section>
      )}

      <fieldset disabled={!esBorrador} className={sectionClass}>
        <legend className="px-1.5 font-semibold text-azul-oscuro">1. Tipo y motivo</legend>
        <div className="flex flex-wrap gap-6">
          <div>
            <span className="mb-1 block text-sm font-semibold text-texto-suave">Tipo de nota</span>
            <div className="flex gap-4">
              {(["NC", "ND"] as const).map((t) => (
                <label key={t} className="flex items-center gap-1.5 text-sm">
                  <input
                    type="radio"
                    className="w-auto"
                    name="tipoNota"
                    checked={tipo === t}
                    disabled={nota !== null}
                    onChange={() => setTipo(t)}
                  />
                  {TIPO_DOCUMENTO_LABEL[t]}
                </label>
              ))}
            </div>
            <p className="mt-1 max-w-sm text-xs text-texto-suave">
              {tipo === "NC"
                ? "Resta de la factura (devolución, descuento…). Si acredita el total, la factura queda anulada."
                : "Suma a la factura (recupero de costos, ajuste de precio en más…)."}
            </p>
          </div>
          <div className="min-w-[240px]">
            <label className="mb-1 block text-sm font-semibold text-texto-suave">Motivo de emisión</label>
            <select value={motivo} onChange={(e) => setMotivo(e.target.value as MotivoNota)}>
              {Object.entries(MOTIVO_NOTA_LABEL).map(([valor, etiqueta]) => (
                <option key={valor} value={valor}>
                  {etiqueta}
                </option>
              ))}
            </select>
          </div>
        </div>
      </fieldset>

      <fieldset disabled={!esBorrador} className={sectionClass}>
        <legend className="px-1.5 font-semibold text-azul-oscuro">2. Ítems de la nota</legend>

        {esBorrador && factura && (
          <div className="mb-3 flex flex-wrap items-center gap-2">
            <select className="max-w-xs" value={itemParaCopiar} onChange={(e) => setItemParaCopiar(e.target.value)}>
              <option value="">Ítem de la factura…</option>
              {factura.items.map((i, indice) => (
                <option key={indice} value={indice}>
                  {i.descripcion} ({i.cantidad} × {i.precioUnitario.toLocaleString("es-PY")})
                </option>
              ))}
            </select>
            <Button type="button" variant="secondary" onClick={copiarItemDeLaFactura} disabled={itemParaCopiar === ""}>
              Agregar
            </Button>
            <Button type="button" variant="secondary" icon={<Plus size={16} />} onClick={agregarManual}>
              Ítem manual
            </Button>
          </div>
        )}

        <div className="overflow-x-auto rounded-lg shadow-card">
          <table className="w-full min-w-[640px] border-collapse text-sm">
            <thead>
              <tr>
                <th className={th}>Descripción</th>
                <th className={th}>Cantidad</th>
                <th className={th}>Precio unit.</th>
                <th className={th}>IVA</th>
                <th className={th}>Subtotal</th>
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
                      onChange={(e) => actualizarItem(item.localId, { precioUnitario: Number(e.target.value) })}
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
                        onClick={() => setItems((prev) => prev.filter((i) => i.localId !== item.localId))}
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

      <section className={sectionClass}>
        <h2 className="m-0 mb-2 text-base font-semibold text-azul-oscuro">3. Totales</h2>
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
              <td className="py-1 font-bold">Total de la nota</td>
              <td className="py-1 font-bold">{totales.totalGeneral.toLocaleString("es-PY")}</td>
            </tr>
          </tbody>
        </table>
        {esBorrador && superaSaldo && (
          <p className="mt-2 text-sm text-rojo">
            Supera el saldo acreditable de la factura ({saldoAcreditable.toLocaleString("es-PY")}). El SIFEN la
            rechazaría (código 2417).
          </p>
        )}
      </section>

      <div className="flex justify-end gap-2">
        {esBorrador && factura && (
          <Button variant="secondary" onClick={guardar} disabled={guardando || superaSaldo}>
            {guardando ? "Guardando…" : "Guardar borrador"}
          </Button>
        )}
        {nota && esBorrador && (
          <Button onClick={confirmarEnvio} disabled={confirmando}>
            {confirmando ? "Enviando…" : "Confirmar y enviar al SIFEN"}
          </Button>
        )}
      </div>
    </div>
  );
}
