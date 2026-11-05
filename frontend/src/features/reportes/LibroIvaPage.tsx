import { FileArchive, FileDown, FileSpreadsheet } from "lucide-react";
import { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { extraerMensajeError } from "../../api/client";
import type { LibroIva, LiquidacionIva, RenglonLibroIva, TipoLibro } from "../../api/types";
import { Badge } from "../../components/ui/Badge";
import { Button } from "../../components/ui/Button";
import { EmptyState } from "../../components/ui/EmptyState";
import { Skeleton } from "../../components/ui/Skeleton";
import { useToast } from "../../components/ui/ToastProvider";
import { descargarArchivo } from "../facturas/api";
import { guaranies, mensajeDeBlob, reportesApi } from "./api";

const TH = "bg-azul-claro px-3 py-2.5 text-left font-semibold text-azul-oscuro whitespace-nowrap";
const TH_NUM = `${TH} text-right`;
const TD = "px-3 py-2";
const TD_NUM = "px-3 py-2 text-right tabular-nums";

type Pestana = TipoLibro | "LIQUIDACION";

const PESTANAS: { valor: Pestana; titulo: string }[] = [
  { valor: "VENTAS", titulo: "Libro Ventas" },
  { valor: "COMPRAS", titulo: "Libro Compras" },
  { valor: "LIQUIDACION", titulo: "Liquidación y RG 90" },
];

function mesActual(): string {
  const hoy = new Date();
  return `${hoy.getFullYear()}-${String(hoy.getMonth() + 1).padStart(2, "0")}`;
}

function origenTitulo(r: RenglonLibroIva) {
  if (r.origen === "SIFEN") return "DE emitido y aprobado por el SIFEN";
  if (r.origen === "DE_RECIBIDO") return "DE recibido del proveedor (con CDC)";
  return "Comprobante registrado a mano";
}

export function LibroIvaPage() {
  const { showToast } = useToast();
  const [params, setParams] = useSearchParams();
  const pestana: Pestana = (["VENTAS", "COMPRAS", "LIQUIDACION"] as const).find((p) => p === params.get("libro")) ?? "VENTAS";
  const [periodo, setPeriodo] = useState(mesActual());
  const [libro, setLibro] = useState<LibroIva | null>(null);
  const [liquidacion, setLiquidacion] = useState<LiquidacionIva | null>(null);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [descargando, setDescargando] = useState<"excel" | "pdf" | "rg90" | null>(null);

  useEffect(() => {
    if (!periodo) return;
    setCargando(true);
    setError(null);
    const pedido =
      pestana === "LIQUIDACION"
        ? reportesApi.liquidacionIva(periodo).then((l) => {
            setLiquidacion(l);
            setLibro(null);
          })
        : reportesApi.libroIva(periodo, pestana).then((l) => {
            setLibro(l);
            setLiquidacion(null);
          });
    pedido
      .catch((err) => setError(extraerMensajeError(err, "No se pudo cargar el reporte")))
      .finally(() => setCargando(false));
  }, [periodo, pestana]);

  function elegir(p: Pestana) {
    setParams(p === "VENTAS" ? {} : { libro: p }, { replace: true });
  }

  async function descargar(formato: "excel" | "pdf") {
    if (pestana === "LIQUIDACION") return;
    setDescargando(formato);
    try {
      const blob = await reportesApi.libroIvaArchivo(periodo, formato, pestana);
      const base = pestana === "VENTAS" ? "libro-iva-ventas" : "libro-iva-compras";
      descargarArchivo(blob, `${base}-${periodo}.${formato === "excel" ? "xlsx" : "pdf"}`);
    } catch {
      showToast("No se pudo descargar el archivo", "error");
    } finally {
      setDescargando(null);
    }
  }

  async function descargarRg90() {
    setDescargando("rg90");
    try {
      const { blob, nombre } = await reportesApi.registroRg90(periodo);
      descargarArchivo(blob, nombre);
      showToast(`Se descargó ${nombre}. Importalo en Marangatu sin descomprimirlo.`, "success");
    } catch (err) {
      showToast(await mensajeDeBlob(err, "No se pudo generar el archivo RG 90"), "error");
    } finally {
      setDescargando(null);
    }
  }

  const esCompras = pestana === "COMPRAS";
  const sinDatos = !libro || libro.renglones.length === 0;

  return (
    <div>
      <h1 className="mb-1 text-xl font-semibold">Libros IVA</h1>
      <p className="mb-4 max-w-3xl text-sm text-texto-suave">
        Ventas: los comprobantes aprobados por el SIFEN más las ventas no electrónicas registradas. Compras: las
        facturas de proveedores imputadas al IVA, cargadas en{" "}
        <Link to="/comprobantes" className="text-azul underline">
          Compras y otros comprobantes
        </Link>
        . Los montos gravados incluyen el IVA y las notas de crédito restan.
      </p>

      <div className="mb-4 flex flex-wrap gap-1 border-b border-borde" role="tablist">
        {PESTANAS.map((p) => (
          <button
            key={p.valor}
            role="tab"
            aria-selected={pestana === p.valor}
            onClick={() => elegir(p.valor)}
            className={`-mb-px rounded-t-md border px-4 py-2 text-sm font-medium ${
              pestana === p.valor
                ? "border-borde border-b-white bg-white text-azul-oscuro"
                : "border-transparent text-texto-suave hover:text-texto"
            }`}
          >
            {p.titulo}
          </button>
        ))}
      </div>

      <div className="mb-4 flex flex-wrap items-end gap-2">
        <label className="text-sm">
          <span className="mb-1 block text-texto-suave">Período</span>
          <input type="month" value={periodo} onChange={(e) => setPeriodo(e.target.value)} className="w-auto" />
        </label>
        {pestana !== "LIQUIDACION" && (
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
        )}
      </div>

      {error && <p className="mb-3 text-sm text-rojo">{error}</p>}

      {pestana === "LIQUIDACION" ? (
        <PanelLiquidacion
          liquidacion={liquidacion}
          cargando={cargando}
          descargando={descargando === "rg90"}
          onDescargarRg90={descargarRg90}
        />
      ) : (
        <>
          {libro && !sinDatos && (
            <div className="mb-4 grid grid-cols-2 gap-3 lg:grid-cols-4">
              <Resumen titulo="Comprobantes" valor={String(libro.totales.cantidadComprobantes)} />
              <Resumen titulo={esCompras ? "Total comprado" : "Total vendido"} valor={`Gs. ${guaranies(libro.totales.total)}`} />
              <Resumen
                titulo="Base imponible 10% / 5%"
                valor={`${guaranies(libro.totales.baseImponible10)} / ${guaranies(libro.totales.baseImponible5)}`}
              />
              <Resumen
                titulo={esCompras ? "IVA crédito fiscal" : "IVA débito fiscal"}
                valor={`Gs. ${guaranies(libro.totales.totalIva)}`}
                destacado
              />
            </div>
          )}

          <div className="overflow-x-auto rounded-lg bg-white shadow-card">
            <table className="w-full min-w-[1040px] border-collapse text-sm">
              <thead>
                <tr>
                  <th className={TH}>Fecha</th>
                  <th className={TH}>Tipo</th>
                  <th className={TH}>Número</th>
                  <th className={TH}>Timbrado</th>
                  <th className={TH}>{esCompras ? "RUC proveedor" : "RUC/CI"}</th>
                  <th className={TH}>{esCompras ? "Proveedor" : "Cliente"}</th>
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
                    <td colSpan={13} className="p-3">
                      <Skeleton rows={5} />
                    </td>
                  </tr>
                ) : sinDatos ? (
                  <tr>
                    <td colSpan={13}>
                      <EmptyState
                        message={
                          esCompras
                            ? "No hay compras imputadas al IVA en este período. Cargalas en Compras y otros comprobantes."
                            : "No hay comprobantes de venta en este período."
                        }
                      />
                    </td>
                  </tr>
                ) : (
                  <>
                    {libro.renglones.map((r) => (
                      <tr key={r.facturaId ?? r.comprobanteRegistradoId} className="border-b border-borde">
                        <td className={TD}>{new Date(`${r.fecha}T00:00:00`).toLocaleDateString("es-PY")}</td>
                        <td className={TD} title={`${r.tipoComprobante} · ${origenTitulo(r)}`}>
                          <span className="font-medium">{r.sigla}</span>
                          {r.origen === "REGISTRADO" && !esCompras && (
                            <span className="ml-1 text-xs text-texto-suave">(preimp.)</span>
                          )}
                        </td>
                        <td className={`${TD} whitespace-nowrap`}>{r.numero}</td>
                        <td className={TD}>{r.timbrado ?? "—"}</td>
                        <td className={TD}>{r.ruc}</td>
                        <td className={TD}>{r.razonSocial}</td>
                        <td className={TD}>
                          {r.documentoAsociado ? (
                            <span className="whitespace-nowrap text-xs">Ajusta {r.documentoAsociado}</span>
                          ) : (
                            r.condicion
                          )}
                        </td>
                        <td className={TD_NUM}>{guaranies(r.gravado10)}</td>
                        <td className={TD_NUM}>{guaranies(r.iva10)}</td>
                        <td className={TD_NUM}>{guaranies(r.gravado5)}</td>
                        <td className={TD_NUM}>{guaranies(r.iva5)}</td>
                        <td className={TD_NUM}>{guaranies(r.exento)}</td>
                        <td className={TD_NUM}>{guaranies(r.total)}</td>
                      </tr>
                    ))}
                    <tr className="bg-azul-claro/40 font-semibold">
                      <td className={TD} colSpan={7}>
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
        </>
      )}
    </div>
  );
}

function PanelLiquidacion({
  liquidacion,
  cargando,
  descargando,
  onDescargarRg90,
}: {
  liquidacion: LiquidacionIva | null;
  cargando: boolean;
  descargando: boolean;
  onDescargarRg90: () => void;
}) {
  if (cargando || !liquidacion) {
    return <Skeleton rows={4} />;
  }
  const aPagar = liquidacion.saldo >= 0;
  return (
    <div className="grid gap-4 lg:grid-cols-2">
      <section className="rounded-lg bg-white p-5 shadow-card">
        <h2 className="mb-3 mt-0 text-base font-semibold text-azul-oscuro">Liquidación del IVA del mes</h2>
        <table className="w-full text-sm">
          <tbody>
            <Fila etiqueta={`Débito fiscal (${liquidacion.comprobantesVenta} ventas)`} valor={liquidacion.debitoFiscal} />
            <Fila etiqueta="   · al 10%" valor={liquidacion.debitoFiscal10} suave />
            <Fila etiqueta="   · al 5%" valor={liquidacion.debitoFiscal5} suave />
            <Fila etiqueta={`Crédito fiscal (${liquidacion.comprobantesCompra} compras)`} valor={-liquidacion.creditoFiscal} />
            <Fila etiqueta="   · al 10%" valor={-liquidacion.creditoFiscal10} suave />
            <Fila etiqueta="   · al 5%" valor={-liquidacion.creditoFiscal5} suave />
            <tr className="border-t border-borde font-semibold">
              <td className="py-2">{aPagar ? "IVA a pagar del período" : "Saldo a favor del contribuyente"}</td>
              <td className={`py-2 text-right tabular-nums ${aPagar ? "text-azul-oscuro" : "text-verde"}`}>
                Gs. {guaranies(Math.abs(liquidacion.saldo))}
              </td>
            </tr>
          </tbody>
        </table>
        <p className="mb-0 mt-3 text-xs text-texto-suave">
          Referencial para el Formulario 120. No incluye el saldo a favor de meses anteriores, retenciones ni
          percepciones: esos se cargan en la declaración.
        </p>
      </section>

      <section className="rounded-lg bg-white p-5 shadow-card">
        <h2 className="mb-1 mt-0 text-base font-semibold text-azul-oscuro">Registro de Comprobantes (RG 90/2021)</h2>
        <p className="mt-0 text-sm text-texto-suave">
          Archivo para la opción <strong>Importar</strong> del Registro Mensual de Comprobantes (obligación 955) en
          Marangatu. Lleva las compras y las ventas no electrónicas del mes; los documentos electrónicos no van, porque
          la SET ya los tiene.
        </p>
        <div className="mb-3 flex flex-wrap gap-2">
          <Badge tone="info">{liquidacion.comprobantesParaRg90} comprobantes van al archivo</Badge>
          {liquidacion.comprobantesElectronicosExcluidos > 0 && (
            <Badge tone="warn">{liquidacion.comprobantesElectronicosExcluidos} DE recibidos excluidos</Badge>
          )}
        </div>
        <Button
          icon={<FileArchive size={16} />}
          disabled={descargando || liquidacion.comprobantesParaRg90 === 0}
          onClick={onDescargarRg90}
        >
          {descargando ? "Generando…" : "Descargar archivo para Marangatu (.zip)"}
        </Button>
        <ol className="mb-0 mt-3 list-decimal pl-5 text-xs text-texto-suave">
          <li>En Marangatu: Registro de Comprobantes → Importar, período del mes.</li>
          <li>Subí el .zip tal como se descargó (el nombre ya tiene el formato RUC_REG_MMAAAA_XXXXX).</li>
          <li>La SET valida el archivo en hasta 72 horas. Las correcciones posteriores se hacen a mano en Marangatu.</li>
        </ol>
      </section>
    </div>
  );
}

function Fila({ etiqueta, valor, suave = false }: { etiqueta: string; valor: number; suave?: boolean }) {
  return (
    <tr className={suave ? "text-texto-suave" : ""}>
      <td className={`py-1 ${suave ? "pl-4 text-xs" : ""}`}>{etiqueta.trim()}</td>
      <td className={`py-1 text-right tabular-nums ${suave ? "text-xs" : ""}`}>
        {valor < 0 ? "− " : ""}
        {guaranies(Math.abs(valor))}
      </td>
    </tr>
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
