import { useEffect, useState, type FormEvent } from "react";
import { extraerMensajeError } from "../../api/client";
import { TASA_IVA_LABEL, type Producto, type TasaIva } from "../../api/types";
import { productosApi, type ProductoInput } from "./api";

const FORM_VACIO: ProductoInput = {
  codigo: "",
  descripcion: "",
  unidadMedida: "UN",
  precioBase: 0,
  tasaIva: "DIEZ",
};

export function ProductosPage() {
  const [productos, setProductos] = useState<Producto[]>([]);
  const [busqueda, setBusqueda] = useState("");
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [mostrarFormulario, setMostrarFormulario] = useState(false);
  const [editandoCodigo, setEditandoCodigo] = useState<string | null>(null);
  const [form, setForm] = useState<ProductoInput>(FORM_VACIO);
  const [errorFormulario, setErrorFormulario] = useState<string | null>(null);

  async function cargar(q?: string) {
    setCargando(true);
    setError(null);
    try {
      setProductos(await productosApi.buscar(q));
    } catch (err) {
      setError(extraerMensajeError(err, "No se pudieron cargar los productos"));
    } finally {
      setCargando(false);
    }
  }

  useEffect(() => {
    cargar();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function abrirNuevo() {
    setEditandoCodigo(null);
    setForm(FORM_VACIO);
    setErrorFormulario(null);
    setMostrarFormulario(true);
  }

  function abrirEdicion(producto: Producto) {
    setEditandoCodigo(producto.codigo);
    setForm({
      codigo: producto.codigo,
      descripcion: producto.descripcion,
      unidadMedida: producto.unidadMedida,
      precioBase: producto.precioBase,
      tasaIva: producto.tasaIva,
    });
    setErrorFormulario(null);
    setMostrarFormulario(true);
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setErrorFormulario(null);
    try {
      if (editandoCodigo) {
        await productosApi.editar(editandoCodigo, form);
      } else {
        await productosApi.crear(form);
      }
      setMostrarFormulario(false);
      await cargar(busqueda);
    } catch (err) {
      setErrorFormulario(extraerMensajeError(err, "No se pudo guardar el producto"));
    }
  }

  async function onDesactivar(producto: Producto) {
    if (!confirm(`¿Desactivar el producto ${producto.codigo}?`)) return;
    try {
      await productosApi.desactivar(producto.codigo);
      await cargar(busqueda);
    } catch (err) {
      setError(extraerMensajeError(err, "No se pudo desactivar el producto"));
    }
  }

  return (
    <div>
      <div className="page-header">
        <h1>Catálogo de productos</h1>
        <button onClick={abrirNuevo}>+ Nuevo producto</button>
      </div>

      <div className="toolbar">
        <input
          placeholder="Buscar por código o descripción…"
          value={busqueda}
          onChange={(e) => setBusqueda(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && cargar(busqueda)}
        />
        <button className="secondary" onClick={() => cargar(busqueda)}>
          Buscar
        </button>
      </div>

      {error && <p className="error-text">{error}</p>}
      {cargando && <p>Cargando…</p>}

      <table className="data-table">
        <thead>
          <tr>
            <th>Código</th>
            <th>Descripción</th>
            <th>Unidad</th>
            <th>Precio base</th>
            <th>Tasa IVA</th>
            <th>Estado</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {productos.map((p) => (
            <tr key={p.codigo}>
              <td>{p.codigo}</td>
              <td>{p.descripcion}</td>
              <td>{p.unidadMedida}</td>
              <td>{p.precioBase.toLocaleString("es-PY")}</td>
              <td>{TASA_IVA_LABEL[p.tasaIva]}</td>
              <td>
                <span className={`badge ${p.activo ? "badge-ok" : "badge-off"}`}>
                  {p.activo ? "Activo" : "Inactivo"}
                </span>
              </td>
              <td className="actions">
                <button className="link-button" onClick={() => abrirEdicion(p)}>
                  Editar
                </button>
                {p.activo && (
                  <button className="link-button danger" onClick={() => onDesactivar(p)}>
                    Desactivar
                  </button>
                )}
              </td>
            </tr>
          ))}
          {!cargando && productos.length === 0 && (
            <tr>
              <td colSpan={7} className="empty-state">
                No hay productos para mostrar.
              </td>
            </tr>
          )}
        </tbody>
      </table>

      {mostrarFormulario && (
        <div className="modal-backdrop" onClick={() => setMostrarFormulario(false)}>
          <form className="modal" onClick={(e) => e.stopPropagation()} onSubmit={onSubmit}>
            <h2>{editandoCodigo ? "Editar producto" : "Nuevo producto"}</h2>

            <label>Código</label>
            <input
              value={form.codigo}
              disabled={!!editandoCodigo}
              onChange={(e) => setForm({ ...form, codigo: e.target.value })}
              required
            />

            <label>Descripción</label>
            <input
              value={form.descripcion}
              onChange={(e) => setForm({ ...form, descripcion: e.target.value })}
              required
            />

            <label>Unidad de medida</label>
            <input
              value={form.unidadMedida}
              onChange={(e) => setForm({ ...form, unidadMedida: e.target.value })}
              required
            />

            <label>Precio base (Gs.)</label>
            <input
              type="number"
              min={0.01}
              step="0.01"
              value={form.precioBase}
              onChange={(e) => setForm({ ...form, precioBase: Number(e.target.value) })}
              required
            />

            <label>Tasa de IVA</label>
            <select
              value={form.tasaIva}
              onChange={(e) => setForm({ ...form, tasaIva: e.target.value as TasaIva })}
            >
              {Object.entries(TASA_IVA_LABEL).map(([valor, etiqueta]) => (
                <option key={valor} value={valor}>
                  {etiqueta}
                </option>
              ))}
            </select>

            {errorFormulario && <p className="error-text">{errorFormulario}</p>}

            <div className="modal-actions">
              <button type="button" className="secondary" onClick={() => setMostrarFormulario(false)}>
                Cancelar
              </button>
              <button type="submit">Guardar</button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
}
