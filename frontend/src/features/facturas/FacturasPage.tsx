import { Download, FileCode2, FileMinus2, Mail, Plus, RotateCcw, SearchCheck } from "lucide-react";
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { extraerMensajeError } from "../../api/client";
import {
  CONDICION_PAGO_LABEL,
  ESTADO_DTE_LABEL,
  TIPO_DOCUMENTO_LABEL,
  type EstadoDte,
  type Factura,
  type TipoDocumento,
} from "../../api/types";
import { Badge } from "../../components/ui/Badge";
import { Button } from "../../components/ui/Button";
import { useConfirm } from "../../components/ui/ConfirmProvider";
import { type Column, DataTable } from "../../components/ui/DataTable";
import { useToast } from "../../components/ui/ToastProvider";
import { descargarArchivo, facturasApi, nombreArchivo } from "./api";
import { resultadoEnvio } from "./VerificarEnvioAviso";

function badgeTone(estado: EstadoDte): "ok" | "off" | "warn" | "info" {
  if (estado === "APROBADO") return "ok";
  if (estado === "RECHAZADO") return "off";
  if (estado === "ANULADO" || estado === "EN_PROCESO") return "warn";
  return "info";
}

/** Facturas y notas comparten el ciclo de vida del DTE, pero se editan en pantallas distintas. */
export function rutaDocumento(f: Pick<Factura, "id" | "tipoDocumento">) {
  return f.tipoDocumento === "FE" ? `/facturas/${f.id}` : `/notas/${f.id}`;
}

export function FacturasPage() {
  const navigate = useNavigate();
  const confirmar = useConfirm();
  const { showToast } = useToast();
  const [facturas, setFacturas] = useState<Factura[]>([]);
  const [filtroEstado, setFiltroEstado] = useState<EstadoDte | "">("");
  const [filtroTipo, setFiltroTipo] = useState<TipoDocumento | "">("");
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function cargar(estado: EstadoDte | "", tipo: TipoDocumento | "") {
    setCargando(true);
    setError(null);
    try {
      setFacturas(await facturasApi.listar(estado, tipo));
    } catch (err) {
      setError(extraerMensajeError(err, "No se pudieron cargar las facturas"));
    } finally {
      setCargando(false);
    }
  }

  useEffect(() => {
    cargar(filtroEstado, filtroTipo);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filtroEstado, filtroTipo]);

  async function onReabrir(f: Factura) {
    if (!(await confirmar("¿Reabrir este documento rechazado para corregirlo y volver a confirmarlo?"))) return;
    try {
      await facturasApi.reabrir(f.id);
      navigate(rutaDocumento(f));
    } catch (err) {
      showToast(extraerMensajeError(err, "No se pudo reabrir el documento"), "error");
    }
  }

  async function onVerificar(f: Factura) {
    try {
      const [mensaje, tipo] = resultadoEnvio(await facturasApi.verificar(f.id), "Documento");
      showToast(mensaje, tipo);
      cargar(filtroEstado, filtroTipo);
    } catch (err) {
      showToast(extraerMensajeError(err, "No se pudo verificar el documento en el SIFEN"), "error");
    }
  }

  async function onDescargar(f: Factura, tipo: "pdf" | "xml") {
    try {
      const blob = tipo === "pdf" ? await facturasApi.descargarPdf(f.id) : await facturasApi.descargarXml(f.id);
      descargarArchivo(blob, nombreArchivo(f, tipo));
    } catch (err) {
      showToast(extraerMensajeError(err, `No se pudo descargar el ${tipo.toUpperCase()}`), "error");
    }
  }

  const columnas: Column<Factura>[] = [
    {
      header: "Tipo",
      render: (f) => (
        <div>
          <span className={f.tipoDocumento === "FE" ? "" : "font-semibold text-azul-oscuro"}>
            {TIPO_DOCUMENTO_LABEL[f.tipoDocumento]}
          </span>
          {f.facturaAsociadaNumero && (
            <div className="text-xs text-texto-suave">de {f.facturaAsociadaNumero}</div>
          )}
        </div>
      ),
      className: "whitespace-nowrap",
    },
    {
      header: "Número",
      render: (f) => f.numero ?? <span className="text-xs text-texto-suave">Borrador</span>,
      className: "whitespace-nowrap",
    },
    { header: "Fecha", render: (f) => new Date(f.fechaEmision).toLocaleString("es-PY") },
    { header: "Cliente", render: (f) => f.clienteRazonSocial },
    {
      header: "Establec. / Punto",
      render: (f) => (
        <span className="text-xs text-texto-suave">
          {f.establecimientoCodigo}-{f.puntoExpedicionCodigo}
        </span>
      ),
    },
    {
      header: "Condición",
      render: (f) => (f.condicionPago ? CONDICION_PAGO_LABEL[f.condicionPago] : <span className="text-texto-suave">—</span>),
    },
    {
      header: "Total",
      render: (f) => (f.tipoDocumento === "NC" ? "−" : "") + f.totalGeneral.toLocaleString("es-PY"),
      className: "whitespace-nowrap",
    },
    {
      header: "Estado",
      render: (f) => (
        <div>
          <Badge tone={badgeTone(f.estadoDte)}>{ESTADO_DTE_LABEL[f.estadoDte]}</Badge>
          {f.emailEnviado && (
            <span className="ml-2 inline-flex items-center gap-1 text-xs text-texto-suave">
              <Mail size={12} /> Enviado
            </span>
          )}
          {(f.estadoDte === "RECHAZADO" || f.estadoDte === "EN_PROCESO") && f.motivoRechazo && (
            <div className="mt-1 text-xs text-texto-suave">{f.motivoRechazo}</div>
          )}
        </div>
      ),
    },
    {
      header: "",
      className: "whitespace-nowrap text-right",
      render: (f) => (
        <>
          <Button variant="link" onClick={() => navigate(rutaDocumento(f))}>
            {f.estadoDte === "BORRADOR" ? "Editar" : "Ver"}
          </Button>
          {f.tipoDocumento === "FE" && f.estadoDte === "APROBADO" && (
            <Button
              variant="link"
              icon={<FileMinus2 size={14} />}
              onClick={() => navigate(`/notas/nueva?factura=${f.id}`)}
            >
              Nota
            </Button>
          )}
          {f.estadoDte === "RECHAZADO" && (
            <Button variant="link" icon={<RotateCcw size={14} />} onClick={() => onReabrir(f)}>
              Reabrir
            </Button>
          )}
          {f.estadoDte === "EN_PROCESO" && (
            <Button variant="link" icon={<SearchCheck size={14} />} onClick={() => onVerificar(f)}>
              Verificar
            </Button>
          )}
          {(f.estadoDte === "APROBADO" || f.estadoDte === "RECHAZADO" || f.estadoDte === "ANULADO") && (
            <>
              <Button variant="link" icon={<Download size={14} />} onClick={() => onDescargar(f, "pdf")}>
                PDF
              </Button>
              <Button variant="link" icon={<FileCode2 size={14} />} onClick={() => onDescargar(f, "xml")}>
                XML
              </Button>
            </>
          )}
        </>
      ),
    },
  ];

  return (
    <div>
      <div className="mb-4 flex items-center justify-between">
        <h1 className="m-0 text-xl font-semibold">Facturación manual</h1>
        <Button icon={<Plus size={16} />} onClick={() => navigate("/facturas/nueva")}>
          Nueva factura
        </Button>
      </div>

      <div className="mb-4 flex flex-wrap gap-3">
        <div className="w-full max-w-xs">
          <select value={filtroTipo} onChange={(e) => setFiltroTipo(e.target.value as TipoDocumento | "")}>
            <option value="">Facturas y notas</option>
            {Object.entries(TIPO_DOCUMENTO_LABEL).map(([valor, etiqueta]) => (
              <option key={valor} value={valor}>
                {etiqueta}
              </option>
            ))}
          </select>
        </div>
        <div className="w-full max-w-xs">
          <select value={filtroEstado} onChange={(e) => setFiltroEstado(e.target.value as EstadoDte | "")}>
            <option value="">Todos los estados</option>
            {Object.entries(ESTADO_DTE_LABEL).map(([valor, etiqueta]) => (
              <option key={valor} value={valor}>
                {etiqueta}
              </option>
            ))}
          </select>
        </div>
      </div>

      {error && <p className="mb-3 text-sm text-rojo">{error}</p>}

      <DataTable
        columns={columnas}
        data={facturas}
        keyExtractor={(f) => f.id}
        loading={cargando}
        emptyMessage="No hay facturas para mostrar."
      />
    </div>
  );
}
