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
import type { Cliente } from "../../api/types";
import { productosApi } from "../catalogo/api";
import type { Producto } from "../../api/types";
import { facturasApi, type FacturaInput, type ItemFacturaInput } from "./api";
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

export function FacturaFormPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [clientes, setClientes] = useState<Cliente[]>([]);
  const [productos, setProductos] = useState<Producto[]>([]);
  const [productoParaAgregar, setProductoParaAgregar] = useState("");

  const [clienteRuc, setClienteRuc] = useState("");
  const [condicionPago, setCondicionPago] = useState<CondicionPago>("CONTADO");
  const [plazoDias, setPlazoDias] = useState<number | "">("");
  const [cantidadCuotas, setCantidadCuotas] = useState<number | "">("");
  const [items, setItems] = useState<FilaItem[]>([]);

  const [facturaId, setFacturaId] = useState<string | null>(null);
  const [estadoDte, setEstadoDte] = useState<EstadoDte | null>(null);
  const [clienteRazonSocial, setClienteRazonSocial] = useState<string | null>(null);

  const [cargando, setCargando] = useState(false);
  const [guardando, setGuardando] = useState(false);
  const [confirmando, setConfirmando] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const esBorrador = estadoDte === null || estadoDte === "BORRADOR";

  useEffect(() => {
    (async () => {
      const [clientesData, productosData] = await Promise.all([clientesApi.buscar(), productosApi.buscar()]);
      setClientes(clientesData);
      setProductos(productosData);
    })().catch((err) => setError(extraerMensajeError(err, "No se pudo cargar catálogo/clientes")));
  }, []);

  useEffect(() => {
    if (!id) return;
    setCargando(true);
    facturasApi
      .obtener(id)
      .then((factura) => {
        setFacturaId(factura.id);
        setEstadoDte(factura.estadoDte);
        setClienteRuc(factura.clienteRuc);
        setClienteRazonSocial(factura.clienteRazonSocial);
        setCondicionPago(factura.condicionPago);
        setPlazoDias(factura.plazoDias ?? "");
        setCantidadCuotas(factura.cantidadCuotas ?? "");
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
      })
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
    if (!confirm("¿Confirmar y enviar esta factura al SIFEN? Ya no se podrá editar.")) return;
    setError(null);
    setConfirmando(true);
    try {
      const factura = await facturasApi.confirmar(facturaId);
      setEstadoDte(factura.estadoDte);
    } catch (err) {
      setError(extraerMensajeError(err, "No se pudo confirmar el envío"));
    } finally {
      setConfirmando(false);
    }
  }

  const totales = calcularTotales(items);

  if (cargando) return <p>Cargando…</p>;

  return (
    <div>
      <div className="page-header">
        <h1>{id ? "Factura" : "Nueva factura manual"}</h1>
        <button className="secondary" onClick={() => navigate("/facturas")}>
          Volver
        </button>
      </div>

      {estadoDte && (
        <p>
          Estado:{" "}
          <span className={`badge ${estadoDte === "APROBADO" ? "badge-ok" : estadoDte === "RECHAZADO" ? "badge-off" : ""}`}>
            {ESTADO_DTE_LABEL[estadoDte]}
          </span>
        </p>
      )}

      {error && <p className="error-text">{error}</p>}

      <fieldset disabled={!esBorrador} className="factura-section">
        <legend>1. Datos del cliente</legend>
        <label>Cliente</label>
        <select value={clienteRuc} onChange={(e) => setClienteRuc(e.target.value)}>
          <option value="">Seleccionar cliente…</option>
          {clientes.map((c) => (
            <option key={c.ruc} value={c.ruc}>
              {c.razonSocial} — {c.ruc}
            </option>
          ))}
        </select>
        {!esBorrador && clienteRazonSocial && <p className="hint-text">{clienteRazonSocial}</p>}
      </fieldset>

      <fieldset disabled={!esBorrador} className="factura-section">
        <legend>2. Ítems de la factura</legend>

        {esBorrador && (
          <div className="toolbar">
            <select value={productoParaAgregar} onChange={(e) => setProductoParaAgregar(e.target.value)}>
              <option value="">Elegir producto del catálogo…</option>
              {productos.map((p) => (
                <option key={p.codigo} value={p.codigo}>
                  {p.descripcion} ({p.codigo})
                </option>
              ))}
            </select>
            <button type="button" className="secondary" onClick={agregarDesdeCatalogo} disabled={!productoParaAgregar}>
              Agregar desde catálogo
            </button>
            <button type="button" className="secondary" onClick={agregarAdHoc}>
              + Ítem manual
            </button>
          </div>
        )}

        <table className="data-table">
          <thead>
            <tr>
              <th>Descripción</th>
              <th>Cantidad</th>
              <th>Precio unit.</th>
              <th>IVA</th>
              <th>Subtotal</th>
              {esBorrador && <th></th>}
            </tr>
          </thead>
          <tbody>
            {items.map((item) => (
              <tr key={item.localId}>
                <td>
                  <input
                    value={item.descripcion}
                    onChange={(e) => actualizarItem(item.localId, { descripcion: e.target.value })}
                  />
                </td>
                <td>
                  <input
                    type="number"
                    min={1}
                    value={item.cantidad}
                    onChange={(e) => actualizarItem(item.localId, { cantidad: Number(e.target.value) })}
                  />
                </td>
                <td>
                  <input
                    type="number"
                    min={0}
                    value={item.precioUnitario}
                    onChange={(e) => actualizarItem(item.localId, { precioUnitario: Number(e.target.value) })}
                  />
                </td>
                <td>
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
                <td>{subtotalItem(item).toLocaleString("es-PY")}</td>
                {esBorrador && (
                  <td>
                    <button type="button" className="link-button danger" onClick={() => quitarItem(item.localId)}>
                      ×
                    </button>
                  </td>
                )}
              </tr>
            ))}
            {items.length === 0 && (
              <tr>
                <td colSpan={6} className="empty-state">
                  Sin ítems todavía.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </fieldset>

      <fieldset disabled={!esBorrador} className="factura-section">
        <legend>3. Condición de pago</legend>
        <label>
          <input
            type="radio"
            name="condicionPago"
            checked={condicionPago === "CONTADO"}
            onChange={() => setCondicionPago("CONTADO")}
          />{" "}
          {CONDICION_PAGO_LABEL.CONTADO}
        </label>{" "}
        <label>
          <input
            type="radio"
            name="condicionPago"
            checked={condicionPago === "CREDITO"}
            onChange={() => setCondicionPago("CREDITO")}
          />{" "}
          {CONDICION_PAGO_LABEL.CREDITO}
        </label>

        {condicionPago === "CREDITO" && (
          <div className="toolbar" style={{ marginTop: 10 }}>
            <div>
              <label>Plazo (días)</label>
              <input
                type="number"
                min={1}
                value={plazoDias}
                onChange={(e) => setPlazoDias(e.target.value === "" ? "" : Number(e.target.value))}
              />
            </div>
            <div>
              <label>Cuotas</label>
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

      <fieldset className="factura-section">
        <legend>4. Totales</legend>
        <table className="data-table">
          <tbody>
            <tr>
              <td>IVA 5%</td>
              <td>{totales.totalIva5.toLocaleString("es-PY")}</td>
            </tr>
            <tr>
              <td>IVA 10%</td>
              <td>{totales.totalIva10.toLocaleString("es-PY")}</td>
            </tr>
            <tr>
              <td>
                <strong>Total general</strong>
              </td>
              <td>
                <strong>{totales.totalGeneral.toLocaleString("es-PY")}</strong>
              </td>
            </tr>
          </tbody>
        </table>
      </fieldset>

      <div className="modal-actions">
        {esBorrador && (
          <button className="secondary" onClick={guardarBorrador} disabled={guardando}>
            {guardando ? "Guardando…" : "Guardar borrador"}
          </button>
        )}
        {facturaId && esBorrador && (
          <button onClick={confirmarEnvio} disabled={confirmando}>
            {confirmando ? "Enviando…" : "Confirmar y enviar al SIFEN"}
          </button>
        )}
      </div>
    </div>
  );
}
