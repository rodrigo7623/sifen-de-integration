import { ChevronLeft, ChevronRight, FilterX, Search } from "lucide-react";
import { useEffect, useState, type FormEvent } from "react";
import { useNavigate } from "react-router-dom";
import { extraerMensajeError } from "../../api/client";
import {
  ESTADO_DTE_LABEL,
  ROL_LABEL,
  TIPO_DOCUMENTO_LABEL,
  type AuditoriaEntrada,
  type EstadoDte,
  type PaginaSpring,
  type TipoDocumento,
  type Usuario,
} from "../../api/types";
import { Badge } from "../../components/ui/Badge";
import { Button } from "../../components/ui/Button";
import { EmptyState } from "../../components/ui/EmptyState";
import { Skeleton } from "../../components/ui/Skeleton";
import { usuariosApi } from "../usuarios/api";
import { auditoriaApi, type FiltrosAuditoria } from "./api";

/** Texto legible de cada operación que registra el backend (AuditoriaService.registrar). */
const OPERACION_LABEL: Record<string, string> = {
  CREAR_BORRADOR: "Creó el borrador",
  EDITAR_BORRADOR: "Editó el borrador",
  CREAR_NOTA_NC: "Creó una nota de crédito",
  CREAR_NOTA_ND: "Creó una nota de débito",
  EDITAR_NOTA: "Editó la nota",
  APROBADO_SIFEN: "Aprobado por el SIFEN",
  RECHAZADO_SIFEN: "Rechazado por el SIFEN",
  ENVIO_SIN_CONFIRMAR: "Envío sin respuesta del SIFEN",
  REABRIR_RECHAZADO: "Reabrió el documento rechazado",
  ANULADO_POR_NC: "Anulado por nota de crédito",
  EMAIL_ENVIADO: "Correo enviado al cliente",
  EMAIL_FALLIDO: "Falló el envío del correo",
};

function etiquetaOperacion(operacion: string): string {
  return OPERACION_LABEL[operacion] ?? operacion;
}

function tonoOperacion(operacion: string): "ok" | "off" | "warn" | "info" {
  if (operacion === "APROBADO_SIFEN" || operacion === "EMAIL_ENVIADO") return "ok";
  if (operacion === "RECHAZADO_SIFEN" || operacion === "EMAIL_FALLIDO") return "off";
  if (operacion === "ENVIO_SIN_CONFIRMAR" || operacion === "ANULADO_POR_NC") return "warn";
  return "info";
}

function tonoEstado(estado: EstadoDte): "ok" | "off" | "warn" | "info" {
  if (estado === "APROBADO") return "ok";
  if (estado === "RECHAZADO") return "off";
  if (estado === "ANULADO" || estado === "EN_PROCESO") return "warn";
  return "info";
}

const FILTROS_VACIOS: FiltrosAuditoria = { desde: "", hasta: "", operacion: "", usuarioId: "", tipoDocumento: "", q: "" };

const TH = "bg-azul-claro px-3.5 py-2.5 text-left font-semibold text-azul-oscuro whitespace-nowrap";

export function AuditoriaPage() {
  const navigate = useNavigate();
  const [pagina, setPagina] = useState<PaginaSpring<AuditoriaEntrada> | null>(null);
  const [numeroPagina, setNumeroPagina] = useState(0);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [borrador, setBorrador] = useState<FiltrosAuditoria>(FILTROS_VACIOS);
  const [filtros, setFiltros] = useState<FiltrosAuditoria>(FILTROS_VACIOS);
  const [operaciones, setOperaciones] = useState<string[]>([]);
  const [usuarios, setUsuarios] = useState<Usuario[]>([]);

  useEffect(() => {
    auditoriaApi.operaciones().then(setOperaciones).catch(() => setOperaciones(Object.keys(OPERACION_LABEL)));
    usuariosApi.buscar().then(setUsuarios).catch(() => setUsuarios([]));
  }, []);

  useEffect(() => {
    setCargando(true);
    setError(null);
    auditoriaApi
      .listar(numeroPagina, filtros)
      .then(setPagina)
      .catch((err) => setError(extraerMensajeError(err, "No se pudo cargar la auditoría")))
      .finally(() => setCargando(false));
  }, [numeroPagina, filtros]);

  function aplicar(e?: FormEvent) {
    e?.preventDefault();
    setNumeroPagina(0);
    setFiltros({ ...borrador });
  }

  function limpiar() {
    setBorrador(FILTROS_VACIOS);
    setNumeroPagina(0);
    setFiltros(FILTROS_VACIOS);
  }

  const hayFiltros = Object.values(filtros).some((v) => v && String(v).trim() !== "");

  function abrirDocumento(entrada: AuditoriaEntrada) {
    if (!entrada.facturaId) return;
    navigate(entrada.documentoTipo && entrada.documentoTipo !== "FE" ? `/notas/${entrada.facturaId}` : `/facturas/${entrada.facturaId}`);
  }

  return (
    <div>
      <h1 className="mb-1 text-xl font-semibold">Auditoría</h1>
      <p className="mb-4 text-sm text-texto-suave">
        Historial de operaciones sobre facturas y notas (RF-08): quién hizo qué, cuándo y sobre qué documento.
      </p>

      <form onSubmit={aplicar} className="mb-4 rounded-lg border border-borde bg-white p-4">
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-6">
          <label className="text-sm font-semibold text-texto-suave lg:col-span-2">
            Documento o cliente
            <span className="relative mt-1 block">
              <Search size={16} className="pointer-events-none absolute left-2.5 top-1/2 -translate-y-1/2 text-texto-suave" />
              <input
                className="pl-8 font-normal"
                placeholder="001-001-0000005, RUC o razón social"
                value={borrador.q}
                maxLength={100}
                onChange={(e) => setBorrador({ ...borrador, q: e.target.value })}
              />
            </span>
          </label>
          <label className="text-sm font-semibold text-texto-suave">
            Desde
            <input
              type="date"
              className="mt-1 font-normal"
              value={borrador.desde}
              max={borrador.hasta || undefined}
              onChange={(e) => setBorrador({ ...borrador, desde: e.target.value })}
            />
          </label>
          <label className="text-sm font-semibold text-texto-suave">
            Hasta
            <input
              type="date"
              className="mt-1 font-normal"
              value={borrador.hasta}
              min={borrador.desde || undefined}
              onChange={(e) => setBorrador({ ...borrador, hasta: e.target.value })}
            />
          </label>
          <label className="text-sm font-semibold text-texto-suave">
            Operación
            <select
              className="mt-1 font-normal"
              value={borrador.operacion}
              onChange={(e) => setBorrador({ ...borrador, operacion: e.target.value })}
            >
              <option value="">Todas</option>
              {operaciones.map((op) => (
                <option key={op} value={op}>
                  {etiquetaOperacion(op)}
                </option>
              ))}
            </select>
          </label>
          <label className="text-sm font-semibold text-texto-suave">
            Usuario
            <select
              className="mt-1 font-normal"
              value={borrador.usuarioId}
              onChange={(e) => setBorrador({ ...borrador, usuarioId: e.target.value })}
            >
              <option value="">Todos</option>
              {usuarios.map((u) => (
                <option key={u.id} value={u.id}>
                  {u.nombre}
                </option>
              ))}
            </select>
          </label>
          <label className="text-sm font-semibold text-texto-suave">
            Tipo de documento
            <select
              className="mt-1 font-normal"
              value={borrador.tipoDocumento}
              onChange={(e) => setBorrador({ ...borrador, tipoDocumento: e.target.value as TipoDocumento | "" })}
            >
              <option value="">Todos</option>
              {Object.entries(TIPO_DOCUMENTO_LABEL).map(([valor, etiqueta]) => (
                <option key={valor} value={valor}>
                  {etiqueta}
                </option>
              ))}
            </select>
          </label>
          <div className="flex items-end gap-2 lg:col-span-5 lg:justify-end">
            {hayFiltros && (
              <Button type="button" variant="secondary" icon={<FilterX size={16} />} onClick={limpiar}>
                Limpiar
              </Button>
            )}
            <Button type="submit" icon={<Search size={16} />}>
              Filtrar
            </Button>
          </div>
        </div>
      </form>

      {error && <p className="mb-3 text-sm text-rojo">{error}</p>}

      <div className="overflow-x-auto rounded-lg bg-white shadow-card">
        <table className="w-full min-w-[960px] border-collapse text-sm">
          <thead>
            <tr>
              <th className={TH}>Fecha y hora</th>
              <th className={TH}>Operación</th>
              <th className={TH}>Usuario</th>
              <th className={TH}>Documento</th>
              <th className={TH}>Cliente</th>
              <th className={`${TH} text-right`}>Total (Gs.)</th>
              <th className={TH}>Estado actual</th>
            </tr>
          </thead>
          <tbody>
            {cargando ? (
              <tr>
                <td colSpan={7} className="p-3">
                  <Skeleton rows={5} />
                </td>
              </tr>
            ) : !pagina || pagina.content.length === 0 ? (
              <tr>
                <td colSpan={7}>
                  <EmptyState
                    message={hayFiltros ? "Ningún registro coincide con los filtros." : "No hay registros de auditoría todavía."}
                  />
                </td>
              </tr>
            ) : (
              pagina.content.map((entrada) => (
                <tr key={entrada.id} className="border-b border-borde align-top last:border-0">
                  <td className="whitespace-nowrap px-3.5 py-2.5">
                    {new Date(entrada.fechaHora).toLocaleString("es-PY", { dateStyle: "short", timeStyle: "medium" })}
                  </td>
                  <td className="px-3.5 py-2.5">
                    <Badge tone={tonoOperacion(entrada.operacion)}>{etiquetaOperacion(entrada.operacion)}</Badge>
                  </td>
                  <td className="px-3.5 py-2.5">
                    {entrada.usuarioNombre ?? "—"}
                    {entrada.usuarioEmail && (
                      <span className="block text-xs text-texto-suave">
                        {entrada.usuarioEmail}
                        {entrada.usuarioRol ? ` · ${ROL_LABEL[entrada.usuarioRol]}` : ""}
                      </span>
                    )}
                  </td>
                  <td className="px-3.5 py-2.5">
                    {entrada.facturaId ? (
                      <button
                        type="button"
                        className="text-left font-medium text-azul underline-offset-2 hover:underline"
                        onClick={() => abrirDocumento(entrada)}
                        title="Abrir el documento"
                      >
                        {entrada.documentoTipo ? TIPO_DOCUMENTO_LABEL[entrada.documentoTipo] : "Documento"}{" "}
                        <span className="whitespace-nowrap">{entrada.documentoNumero ?? "(borrador)"}</span>
                      </button>
                    ) : (
                      "—"
                    )}
                  </td>
                  <td className="px-3.5 py-2.5">
                    {entrada.clienteRazonSocial ?? "—"}
                    {entrada.clienteRuc && <span className="block text-xs text-texto-suave">{entrada.clienteRuc}</span>}
                  </td>
                  <td className="whitespace-nowrap px-3.5 py-2.5 text-right">
                    {entrada.documentoTotal !== null ? entrada.documentoTotal.toLocaleString("es-PY") : "—"}
                  </td>
                  <td className="px-3.5 py-2.5">
                    {entrada.documentoEstado ? (
                      <Badge tone={tonoEstado(entrada.documentoEstado)}>{ESTADO_DTE_LABEL[entrada.documentoEstado]}</Badge>
                    ) : (
                      "—"
                    )}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>

        {pagina && pagina.totalElements > 0 && (
          <div className="flex items-center justify-between border-t border-borde px-3.5 py-2.5 text-sm text-texto-suave">
            <span>
              Página {pagina.number + 1} de {Math.max(pagina.totalPages, 1)} · {pagina.totalElements} registros
            </span>
            <div className="flex gap-1">
              <button
                className="rounded-md p-1 hover:bg-azul-claro disabled:opacity-40"
                disabled={numeroPagina === 0}
                onClick={() => setNumeroPagina((p) => p - 1)}
                aria-label="Página anterior"
              >
                <ChevronLeft size={18} />
              </button>
              <button
                className="rounded-md p-1 hover:bg-azul-claro disabled:opacity-40"
                disabled={numeroPagina >= pagina.totalPages - 1}
                onClick={() => setNumeroPagina((p) => p + 1)}
                aria-label="Página siguiente"
              >
                <ChevronRight size={18} />
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
