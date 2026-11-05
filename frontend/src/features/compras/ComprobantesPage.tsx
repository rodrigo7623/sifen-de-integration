import { Pencil, Plus, Trash2 } from "lucide-react";
import { useEffect, useMemo, useState, type FormEvent } from "react";
import { Link } from "react-router-dom";
import { extraerMensajeError } from "../../api/client";
import type {
  CatalogoRg90,
  ComprobanteRegistrado,
  ComprobanteRegistradoInput,
  TipoComprobanteRg90,
  TipoRegistro,
} from "../../api/types";
import { useAuth } from "../../auth/AuthContext";
import { Badge } from "../../components/ui/Badge";
import { Button } from "../../components/ui/Button";
import { useConfirm } from "../../components/ui/ConfirmProvider";
import { type Column, DataTable } from "../../components/ui/DataTable";
import { FormField } from "../../components/ui/FormField";
import { Modal } from "../../components/ui/Modal";
import { MontoInput } from "../../components/ui/MontoInput";
import { useToast } from "../../components/ui/ToastProvider";
import { guaranies } from "../reportes/api";
import { comprobantesApi } from "./api";

function mesActual(): string {
  const hoy = new Date();
  return `${hoy.getFullYear()}-${String(hoy.getMonth() + 1).padStart(2, "0")}`;
}

function hoyIso(): string {
  const hoy = new Date();
  return `${hoy.getFullYear()}-${String(hoy.getMonth() + 1).padStart(2, "0")}-${String(hoy.getDate()).padStart(2, "0")}`;
}

/** Tottal Store liquida IVA e IRE: por defecto, cada comprobante se imputa a los dos. */
function formVacio(tipoRegistro: TipoRegistro): ComprobanteRegistradoInput {
  return {
    tipoRegistro,
    tipoComprobante: "FACTURA",
    fechaEmision: hoyIso(),
    tipoIdentificacion: "RUC",
    identificacion: "",
    razonSocial: "",
    timbrado: "",
    numero: "",
    cdc: "",
    condicion: "CONTADO",
    monedaExtranjera: false,
    gravado10: 0,
    gravado5: 0,
    exento: 0,
    total: 0,
    imputaIva: true,
    imputaIre: true,
    imputaIrp: false,
    noImputa: false,
    asociadoNumero: "",
    asociadoTimbrado: "",
    observacion: "",
  };
}

const NUMERO_PATRON = "\\d{3}-\\d{3}-\\d{7}";

/**
 * Registro de comprobantes que no emite el SIFEN de Tottal Store (RG 90/2021): las compras (que dan el
 * crédito fiscal del Libro IVA Compras) y las ventas hechas con comprobantes preimpresos o tickets.
 */
export function ComprobantesPage() {
  const { usuario } = useAuth();
  const esAdmin = usuario?.rol === "ADMIN";
  const confirmar = useConfirm();
  const { showToast } = useToast();

  const [periodo, setPeriodo] = useState(mesActual());
  const [filtroTipo, setFiltroTipo] = useState<TipoRegistro | "">("");
  const [comprobantes, setComprobantes] = useState<ComprobanteRegistrado[]>([]);
  const [catalogo, setCatalogo] = useState<CatalogoRg90 | null>(null);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [editandoId, setEditandoId] = useState<string | null>(null);
  const [form, setForm] = useState<ComprobanteRegistradoInput | null>(null);
  const [errorFormulario, setErrorFormulario] = useState<string | null>(null);
  const [guardando, setGuardando] = useState(false);

  useEffect(() => {
    comprobantesApi
      .catalogo()
      .then(setCatalogo)
      .catch(() => setError("No se pudieron cargar los tipos de comprobante"));
  }, []);

  async function cargar() {
    if (!periodo) return;
    setCargando(true);
    setError(null);
    try {
      setComprobantes(await comprobantesApi.listar(periodo, filtroTipo || undefined));
    } catch (err) {
      setError(extraerMensajeError(err, "No se pudieron cargar los comprobantes"));
    } finally {
      setCargando(false);
    }
  }

  useEffect(() => {
    cargar();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [periodo, filtroTipo]);

  const tipoActual: TipoComprobanteRg90 | undefined = useMemo(
    () => catalogo?.tiposComprobante.find((t) => t.valor === form?.tipoComprobante),
    [catalogo, form?.tipoComprobante],
  );
  const esCompra = form?.tipoRegistro === "COMPRA";
  const soloTotal = !!(esCompra && tipoActual?.soloTotalEnCompras);
  const identificacionActual = catalogo?.tiposIdentificacion.find((t) => t.valor === form?.tipoIdentificacion);

  function siglaDe(valor: string) {
    return catalogo?.tiposComprobante.find((t) => t.valor === valor)?.sigla ?? valor;
  }

  function abrirNuevo(tipo: TipoRegistro) {
    setEditandoId(null);
    setForm(formVacio(tipo));
    setErrorFormulario(null);
  }

  function abrirEdicion(c: ComprobanteRegistrado) {
    setEditandoId(c.id);
    setForm({
      tipoRegistro: c.tipoRegistro,
      tipoComprobante: c.tipoComprobante,
      fechaEmision: c.fechaEmision,
      tipoIdentificacion: c.tipoIdentificacion,
      identificacion: c.identificacion,
      razonSocial: c.razonSocial ?? "",
      timbrado: c.timbrado === "0" ? "" : c.timbrado,
      numero: c.numero ?? "",
      cdc: c.cdc ?? "",
      condicion: c.condicion,
      monedaExtranjera: c.monedaExtranjera,
      gravado10: c.gravado10,
      gravado5: c.gravado5,
      exento: c.exento,
      total: c.total,
      imputaIva: c.imputaIva,
      imputaIre: c.imputaIre,
      imputaIrp: c.imputaIrp,
      noImputa: c.noImputa,
      asociadoNumero: c.asociadoNumero ?? "",
      asociadoTimbrado: c.asociadoTimbrado ?? "",
      observacion: c.observacion ?? "",
    });
    setErrorFormulario(null);
  }

  function actualizar(cambios: Partial<ComprobanteRegistradoInput>) {
    setForm((f) => {
      if (!f) return f;
      const nuevo = { ...f, ...cambios };
      const tipo = catalogo?.tiposComprobante.find((t) => t.valor === nuevo.tipoComprobante);
      // Si el tipo no corresponde al registro elegido, se vuelve a Factura.
      if (tipo && !(nuevo.tipoRegistro === "COMPRA" ? tipo.compras : tipo.ventas)) {
        nuevo.tipoComprobante = "FACTURA";
      }
      // En compras el proveedor va siempre con RUC, salvo autofactura y despacho de importación.
      const tipoFinal = catalogo?.tiposComprobante.find((t) => t.valor === nuevo.tipoComprobante);
      if (nuevo.tipoRegistro === "COMPRA" && tipoFinal?.exigeRucEnCompras) {
        nuevo.tipoIdentificacion = "RUC";
      }
      // El total sale de la suma por tasa, salvo los comprobantes de compra que solo informan el total.
      const soloTotalNuevo = nuevo.tipoRegistro === "COMPRA" && !!tipoFinal?.soloTotalEnCompras;
      if (!soloTotalNuevo) {
        nuevo.total = (nuevo.gravado10 || 0) + (nuevo.gravado5 || 0) + (nuevo.exento || 0);
      }
      if (nuevo.tipoRegistro === "VENTA") {
        nuevo.cdc = "";
        nuevo.noImputa = false;
      }
      return nuevo;
    });
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    if (!form) return;
    setGuardando(true);
    setErrorFormulario(null);
    const vacioANull = (v: string | null) => (v && v.trim() !== "" ? v.trim() : null);
    const datos: ComprobanteRegistradoInput = {
      ...form,
      razonSocial: vacioANull(form.razonSocial),
      timbrado: vacioANull(form.timbrado),
      numero: vacioANull(form.numero),
      cdc: vacioANull(form.cdc),
      asociadoNumero: tipoActual?.esNota ? vacioANull(form.asociadoNumero) : null,
      asociadoTimbrado: tipoActual?.esNota ? vacioANull(form.asociadoTimbrado) : null,
      observacion: vacioANull(form.observacion),
      gravado10: soloTotal ? 0 : form.gravado10 || 0,
      gravado5: soloTotal ? 0 : form.gravado5 || 0,
      exento: soloTotal ? 0 : form.exento || 0,
    };
    try {
      if (editandoId) {
        await comprobantesApi.editar(editandoId, datos);
        showToast("Comprobante actualizado", "success");
      } else {
        await comprobantesApi.crear(datos);
        showToast("Comprobante registrado", "success");
      }
      setForm(null);
      cargar();
    } catch (err) {
      setErrorFormulario(extraerMensajeError(err, "No se pudo guardar el comprobante"));
    } finally {
      setGuardando(false);
    }
  }

  async function eliminar(c: ComprobanteRegistrado) {
    const ok = await confirmar(
      `¿Eliminar ${c.tipoComprobanteDescripcion.toLowerCase()} ${c.numero ?? ""} de ${c.razonSocial ?? c.identificacion}? ` +
        "Si ya se importó en Marangatu, la corrección allá se hace a mano.",
    );
    if (!ok) return;
    try {
      await comprobantesApi.eliminar(c.id);
      showToast("Comprobante eliminado", "success");
      cargar();
    } catch (err) {
      showToast(extraerMensajeError(err, "No se pudo eliminar el comprobante"), "error");
    }
  }

  const columnas: Column<ComprobanteRegistrado>[] = [
    { header: "Fecha", render: (c) => new Date(`${c.fechaEmision}T00:00:00`).toLocaleDateString("es-PY") },
    {
      header: "Registro",
      render: (c) => <Badge tone={c.tipoRegistro === "COMPRA" ? "info" : "ok"}>{c.tipoRegistro === "COMPRA" ? "Compra" : "Venta"}</Badge>,
    },
    {
      header: "Comprobante",
      render: (c) => (
        <span title={c.tipoComprobanteDescripcion}>
          <span className="font-semibold">{siglaDe(c.tipoComprobante)}</span> {c.numero ?? "s/n"}
          <span className="block text-xs text-texto-suave">Timbrado {c.timbrado}</span>
        </span>
      ),
    },
    {
      header: "Contraparte",
      render: (c) => (
        <span>
          {c.razonSocial ?? "—"}
          <span className="block text-xs text-texto-suave">{c.identificacion}</span>
        </span>
      ),
    },
    { header: "Total", className: "text-right tabular-nums", render: (c) => guaranies(c.total) },
    { header: "IVA", className: "text-right tabular-nums", render: (c) => guaranies(c.iva10 + c.iva5) },
    {
      header: "Imputa",
      render: (c) => (
        <span className="text-xs">
          {[c.imputaIva && "IVA", c.imputaIre && "IRE", c.imputaIrp && "IRP-RSP"].filter(Boolean).join(" · ")}
        </span>
      ),
    },
    {
      header: "RG 90",
      render: (c) =>
        c.electronico ? (
          <span title="Es un DE: la SET ya lo tiene y no va en el archivo de importación">
            <Badge tone="warn">DE recibido</Badge>
          </span>
        ) : (
          <Badge tone="info">Va al archivo</Badge>
        ),
    },
    {
      header: "",
      render: (c) => (
        <div className="flex justify-end gap-1">
          <Button variant="link" icon={<Pencil size={14} />} onClick={() => abrirEdicion(c)}>
            Editar
          </Button>
          {esAdmin && (
            <Button variant="link-danger" icon={<Trash2 size={14} />} onClick={() => eliminar(c)}>
              Eliminar
            </Button>
          )}
        </div>
      ),
    },
  ];

  const tiposDelRegistro =
    catalogo?.tiposComprobante.filter((t) => (form?.tipoRegistro === "COMPRA" ? t.compras : t.ventas)) ?? [];
  const ivaPrevisto = soloTotal ? 0 : Math.round((form?.gravado10 || 0) / 11) + Math.round((form?.gravado5 || 0) / 21);

  return (
    <div>
      <div className="mb-1 flex flex-wrap items-center justify-between gap-2">
        <h1 className="m-0 text-xl font-semibold">Compras y otros comprobantes</h1>
        <div className="flex gap-2">
          <Button icon={<Plus size={16} />} onClick={() => abrirNuevo("COMPRA")}>
            Registrar compra
          </Button>
          <Button variant="secondary" icon={<Plus size={16} />} onClick={() => abrirNuevo("VENTA")}>
            Venta no electrónica
          </Button>
        </div>
      </div>
      <p className="mb-4 max-w-3xl text-sm text-texto-suave">
        Comprobantes que no salen del SIFEN de Tottal Store, para el Registro de Comprobantes de la RG 90/2021: las
        facturas de proveedores (crédito fiscal del <Link to="/reportes/libro-iva?libro=COMPRAS" className="text-azul underline">Libro IVA Compras</Link>)
        y las ventas con comprobantes preimpresos o tickets. Las facturas electrónicas propias no se cargan acá: ya
        están en el Libro IVA Ventas.
      </p>

      <div className="mb-4 flex flex-wrap items-end gap-3">
        <label className="text-sm">
          <span className="mb-1 block text-texto-suave">Período</span>
          <input type="month" value={periodo} onChange={(e) => setPeriodo(e.target.value)} className="w-auto" />
        </label>
        <label className="text-sm">
          <span className="mb-1 block text-texto-suave">Mostrar</span>
          <select value={filtroTipo} onChange={(e) => setFiltroTipo(e.target.value as TipoRegistro | "")} className="w-auto">
            <option value="">Compras y ventas</option>
            <option value="COMPRA">Solo compras</option>
            <option value="VENTA">Solo ventas</option>
          </select>
        </label>
      </div>

      {error && <p className="mb-3 text-sm text-rojo">{error}</p>}

      <DataTable
        columns={columnas}
        data={comprobantes}
        keyExtractor={(c) => c.id}
        loading={cargando}
        emptyMessage="No hay comprobantes registrados en este período."
        pageSize={15}
      />

      {form && (
        <Modal
          title={`${editandoId ? "Editar" : "Registrar"} ${esCompra ? "compra" : "venta no electrónica"}`}
          onClose={() => setForm(null)}
          maxWidthClassName="max-w-3xl"
        >
          <form onSubmit={onSubmit}>
            <div className="grid gap-x-4 sm:grid-cols-3">
              <FormField label="Registro">
                <select
                  value={form.tipoRegistro}
                  onChange={(e) => actualizar({ tipoRegistro: e.target.value as TipoRegistro })}
                >
                  <option value="COMPRA">Compra</option>
                  <option value="VENTA">Venta (preimpresa / ticket)</option>
                </select>
              </FormField>
              <FormField label="Tipo de comprobante">
                <select value={form.tipoComprobante} onChange={(e) => actualizar({ tipoComprobante: e.target.value })}>
                  {tiposDelRegistro.map((t) => (
                    <option key={t.valor} value={t.valor}>
                      {t.codigo} · {t.descripcion}
                    </option>
                  ))}
                </select>
              </FormField>
              <FormField label="Fecha de emisión">
                <input
                  type="date"
                  value={form.fechaEmision}
                  max={hoyIso()}
                  onChange={(e) => actualizar({ fechaEmision: e.target.value })}
                  required
                />
              </FormField>
            </div>

            <div className="grid gap-x-4 sm:grid-cols-3">
              <FormField label={esCompra ? "Identificación del proveedor" : "Identificación del cliente"}>
                <select
                  value={form.tipoIdentificacion}
                  onChange={(e) => actualizar({ tipoIdentificacion: e.target.value })}
                  disabled={esCompra && tipoActual?.exigeRucEnCompras}
                  title={esCompra && tipoActual?.exigeRucEnCompras ? "En compras el proveedor se identifica con su RUC" : undefined}
                >
                  {catalogo?.tiposIdentificacion.map((t) => (
                    <option key={t.valor} value={t.valor}>
                      {t.descripcion}
                    </option>
                  ))}
                </select>
              </FormField>
              <FormField label={form.tipoIdentificacion === "RUC" ? "RUC (con DV)" : "Número"}>
                <input
                  value={form.identificacion}
                  onChange={(e) => actualizar({ identificacion: e.target.value })}
                  placeholder={form.tipoIdentificacion === "RUC" ? "80012345-6" : ""}
                  maxLength={20}
                  required
                />
              </FormField>
              <FormField label={identificacionActual?.nombreOpcional ? "Razón social (opcional)" : "Razón social"}>
                <input
                  value={form.razonSocial ?? ""}
                  onChange={(e) => actualizar({ razonSocial: e.target.value })}
                  maxLength={250}
                  required={!identificacionActual?.nombreOpcional}
                />
              </FormField>
            </div>

            <div className="grid gap-x-4 sm:grid-cols-3">
              {!tipoActual?.sinTimbrado && (
                <FormField label="Timbrado">
                  <input
                    value={form.timbrado ?? ""}
                    onChange={(e) => actualizar({ timbrado: e.target.value.replace(/\D/g, "") })}
                    inputMode="numeric"
                    pattern="\d{8}"
                    title="8 dígitos"
                    maxLength={8}
                    required
                  />
                </FormField>
              )}
              <FormField label={tipoActual?.numeroOpcional ? "Número (opcional)" : "Número de comprobante"}>
                <input
                  value={form.numero ?? ""}
                  onChange={(e) => actualizar({ numero: e.target.value })}
                  placeholder="001-001-0000001"
                  pattern={NUMERO_PATRON}
                  title="Formato 001-001-0000001"
                  maxLength={15}
                  required={!tipoActual?.numeroOpcional}
                />
              </FormField>
              {!tipoActual?.esNota && (
                <FormField label="Condición">
                  <select
                    value={form.condicion}
                    onChange={(e) => actualizar({ condicion: e.target.value as "CONTADO" | "CREDITO" })}
                  >
                    <option value="CONTADO">Contado</option>
                    <option value="CREDITO">Crédito</option>
                  </select>
                </FormField>
              )}
            </div>

            {esCompra && (
              <FormField label="CDC (si la compra es una factura electrónica)">
                <input
                  value={form.cdc ?? ""}
                  onChange={(e) => actualizar({ cdc: e.target.value.replace(/\D/g, "") })}
                  inputMode="numeric"
                  pattern="\d{44}"
                  title="44 dígitos"
                  maxLength={44}
                  placeholder="Opcional: con CDC va al Libro IVA Compras pero no al archivo RG 90"
                />
              </FormField>
            )}

            {tipoActual?.esNota && (
              <div className="grid gap-x-4 sm:grid-cols-2">
                <FormField label="Número del comprobante que ajusta">
                  <input
                    value={form.asociadoNumero ?? ""}
                    onChange={(e) => actualizar({ asociadoNumero: e.target.value })}
                    placeholder="001-001-0000001"
                    pattern={NUMERO_PATRON}
                    title="Formato 001-001-0000001"
                    maxLength={15}
                    required
                  />
                </FormField>
                <FormField label="Timbrado del comprobante que ajusta">
                  <input
                    value={form.asociadoTimbrado ?? ""}
                    onChange={(e) => actualizar({ asociadoTimbrado: e.target.value.replace(/\D/g, "") })}
                    inputMode="numeric"
                    pattern="\d{8}"
                    title="8 dígitos"
                    maxLength={8}
                    required
                  />
                </FormField>
              </div>
            )}

            {soloTotal ? (
              <div className="grid gap-x-4 sm:grid-cols-3">
                <FormField label="Total">
                  <MontoInput value={form.total || null} onChange={(v) => actualizar({ total: v ?? 0 })} required />
                </FormField>
                <p className="text-sm text-texto-suave sm:col-span-2 sm:mt-7">
                  En compras, este comprobante solo informa el total y no genera crédito fiscal.
                </p>
              </div>
            ) : (
              <div className="grid gap-x-4 sm:grid-cols-4">
                <FormField label="Gravado 10% (IVA incl.)">
                  <MontoInput value={form.gravado10 || null} onChange={(v) => actualizar({ gravado10: v ?? 0 })} />
                </FormField>
                <FormField label="Gravado 5% (IVA incl.)">
                  <MontoInput value={form.gravado5 || null} onChange={(v) => actualizar({ gravado5: v ?? 0 })} />
                </FormField>
                <FormField label="Exento">
                  <MontoInput value={form.exento || null} onChange={(v) => actualizar({ exento: v ?? 0 })} />
                </FormField>
                <FormField label="Total">
                  <div className="rounded-md border border-borde bg-slate-50 px-2.5 py-2 text-sm tabular-nums">
                    {guaranies(form.total)}
                  </div>
                </FormField>
              </div>
            )}
            {!soloTotal && (
              <p className="-mt-1 mb-3 text-sm text-texto-suave">
                IVA incluido: Gs. {guaranies(ivaPrevisto)}
                {esCompra && form.imputaIva ? " (crédito fiscal)" : ""}
              </p>
            )}

            <fieldset className="mb-3 rounded-md border border-borde px-3 py-2">
              <legend className="px-1 text-sm font-semibold text-texto-suave">Imputa a</legend>
              <div className="flex flex-wrap gap-x-5 gap-y-1 text-sm">
                <Check label="IVA" checked={form.imputaIva} onChange={(v) => actualizar({ imputaIva: v })} />
                <Check label="IRE" checked={form.imputaIre} onChange={(v) => actualizar({ imputaIre: v })} />
                <Check label="IRP-RSP" checked={form.imputaIrp} onChange={(v) => actualizar({ imputaIrp: v })} />
                {esCompra && (
                  <Check
                    label="Una parte no se imputa"
                    checked={form.noImputa}
                    onChange={(v) => actualizar({ noImputa: v })}
                  />
                )}
                <Check
                  label="Operación en moneda extranjera"
                  checked={form.monedaExtranjera}
                  onChange={(v) => actualizar({ monedaExtranjera: v })}
                />
              </div>
            </fieldset>

            <FormField label="Observación (uso interno)">
              <input
                value={form.observacion ?? ""}
                onChange={(e) => actualizar({ observacion: e.target.value })}
                maxLength={250}
              />
            </FormField>

            {errorFormulario && <p className="mb-2 text-sm text-rojo">{errorFormulario}</p>}

            <div className="mt-4 flex justify-end gap-2">
              <Button type="button" variant="secondary" onClick={() => setForm(null)}>
                Cancelar
              </Button>
              <Button type="submit" disabled={guardando}>
                {guardando ? "Guardando…" : "Guardar"}
              </Button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  );
}

function Check({ label, checked, onChange }: { label: string; checked: boolean; onChange: (v: boolean) => void }) {
  return (
    <label className="flex items-center gap-1.5">
      <input type="checkbox" className="w-auto" checked={checked} onChange={(e) => onChange(e.target.checked)} />
      {label}
    </label>
  );
}
