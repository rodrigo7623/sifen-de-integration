import { Download, FileCode2, Mail, Plus, RotateCcw } from "lucide-react";
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { extraerMensajeError } from "../../api/client";
import { ESTADO_DTE_LABEL, type EstadoDte, type Factura } from "../../api/types";
import { Badge } from "../../components/ui/Badge";
import { Button } from "../../components/ui/Button";
import { useConfirm } from "../../components/ui/ConfirmProvider";
import { type Column, DataTable } from "../../components/ui/DataTable";
import { useToast } from "../../components/ui/ToastProvider";
import { descargarArchivo, facturasApi } from "./api";

function badgeTone(estado: EstadoDte): "ok" | "off" | "info" {
  if (estado === "APROBADO") return "ok";
  if (estado === "RECHAZADO") return "off";
  return "info";
}

export function FacturasPage() {
  const navigate = useNavigate();
  const confirmar = useConfirm();
  const { showToast } = useToast();
  const [facturas, setFacturas] = useState<Factura[]>([]);
  const [filtroEstado, setFiltroEstado] = useState<EstadoDte | "">("");
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function cargar(estado: EstadoDte | "") {
    setCargando(true);
    setError(null);
    try {
      setFacturas(await facturasApi.listar(estado));
    } catch (err) {
      setError(extraerMensajeError(err, "No se pudieron cargar las facturas"));
    } finally {
      setCargando(false);
    }
  }

  useEffect(() => {
    cargar(filtroEstado);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filtroEstado]);

  async function onReabrir(f: Factura) {
    if (!(await confirmar("¿Reabrir esta factura rechazada para corregirla y volver a confirmarla?"))) return;
    try {
      await facturasApi.reabrir(f.id);
      navigate(`/facturas/${f.id}`);
    } catch (err) {
      showToast(extraerMensajeError(err, "No se pudo reabrir la factura"), "error");
    }
  }

  async function onDescargar(f: Factura, tipo: "pdf" | "xml") {
    try {
      const blob = tipo === "pdf" ? await facturasApi.descargarPdf(f.id) : await facturasApi.descargarXml(f.id);
      descargarArchivo(blob, `factura-${f.id}.${tipo}`);
    } catch (err) {
      showToast(extraerMensajeError(err, `No se pudo descargar el ${tipo.toUpperCase()}`), "error");
    }
  }

  const columnas: Column<Factura>[] = [
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
    { header: "Condición", render: (f) => f.condicionPago },
    { header: "Total", render: (f) => f.totalGeneral.toLocaleString("es-PY") },
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
          {f.estadoDte === "RECHAZADO" && f.motivoRechazo && (
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
          <Button variant="link" onClick={() => navigate(`/facturas/${f.id}`)}>
            {f.estadoDte === "BORRADOR" ? "Editar" : "Ver"}
          </Button>
          {f.estadoDte === "RECHAZADO" && (
            <Button variant="link" icon={<RotateCcw size={14} />} onClick={() => onReabrir(f)}>
              Reabrir
            </Button>
          )}
          {(f.estadoDte === "APROBADO" || f.estadoDte === "RECHAZADO") && (
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

      <div className="mb-4 max-w-xs">
        <select value={filtroEstado} onChange={(e) => setFiltroEstado(e.target.value as EstadoDte | "")}>
          <option value="">Todos los estados</option>
          {Object.entries(ESTADO_DTE_LABEL).map(([valor, etiqueta]) => (
            <option key={valor} value={valor}>
              {etiqueta}
            </option>
          ))}
        </select>
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
