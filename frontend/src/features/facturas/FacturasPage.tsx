import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { extraerMensajeError } from "../../api/client";
import { ESTADO_DTE_LABEL, type EstadoDte, type Factura } from "../../api/types";
import { facturasApi } from "./api";

export function FacturasPage() {
  const navigate = useNavigate();
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

  return (
    <div>
      <div className="page-header">
        <h1>Facturación manual</h1>
        <button onClick={() => navigate("/facturas/nueva")}>+ Nueva factura</button>
      </div>

      <div className="toolbar">
        <select value={filtroEstado} onChange={(e) => setFiltroEstado(e.target.value as EstadoDte | "")}>
          <option value="">Todos los estados</option>
          {Object.entries(ESTADO_DTE_LABEL).map(([valor, etiqueta]) => (
            <option key={valor} value={valor}>
              {etiqueta}
            </option>
          ))}
        </select>
      </div>

      {error && <p className="error-text">{error}</p>}
      {cargando && <p>Cargando…</p>}

      <table className="data-table">
        <thead>
          <tr>
            <th>Fecha</th>
            <th>Cliente</th>
            <th>Condición</th>
            <th>Total</th>
            <th>Estado</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {facturas.map((f) => (
            <tr key={f.id}>
              <td>{new Date(f.fechaEmision).toLocaleString("es-PY")}</td>
              <td>{f.clienteRazonSocial}</td>
              <td>{f.condicionPago}</td>
              <td>{f.totalGeneral.toLocaleString("es-PY")}</td>
              <td>
                <span
                  className={`badge ${
                    f.estadoDte === "APROBADO" ? "badge-ok" : f.estadoDte === "RECHAZADO" ? "badge-off" : ""
                  }`}
                >
                  {ESTADO_DTE_LABEL[f.estadoDte]}
                </span>
              </td>
              <td className="actions">
                <button className="link-button" onClick={() => navigate(`/facturas/${f.id}`)}>
                  {f.estadoDte === "BORRADOR" ? "Editar" : "Ver"}
                </button>
              </td>
            </tr>
          ))}
          {!cargando && facturas.length === 0 && (
            <tr>
              <td colSpan={6} className="empty-state">
                No hay facturas para mostrar.
              </td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  );
}
