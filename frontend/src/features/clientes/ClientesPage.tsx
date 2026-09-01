import { useEffect, useState, type FormEvent } from "react";
import { extraerMensajeError } from "../../api/client";
import { useAuth } from "../../auth/AuthContext";
import { CONDICION_IVA_LABEL, type Cliente, type CondicionIva } from "../../api/types";
import { clientesApi, type ClienteInput } from "./api";

const FORM_VACIO: ClienteInput = {
  ruc: "",
  razonSocial: "",
  direccion: "",
  email: "",
  condicionIva: "RESPONSABLE_IVA",
};

export function ClientesPage() {
  const { usuario } = useAuth();
  const esAdmin = usuario?.rol === "ADMIN";
  const [clientes, setClientes] = useState<Cliente[]>([]);
  const [busqueda, setBusqueda] = useState("");
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [mostrarFormulario, setMostrarFormulario] = useState(false);
  const [editandoRuc, setEditandoRuc] = useState<string | null>(null);
  const [form, setForm] = useState<ClienteInput>(FORM_VACIO);
  const [errorFormulario, setErrorFormulario] = useState<string | null>(null);
  const [estadoRuc, setEstadoRuc] = useState<{ validando: boolean; mensaje: string | null; valido: boolean }>({
    validando: false,
    mensaje: null,
    valido: true,
  });
  const [mostrarInactivos, setMostrarInactivos] = useState(false);

  async function cargar(q?: string, incluirInactivos = mostrarInactivos) {
    setCargando(true);
    setError(null);
    try {
      setClientes(await clientesApi.buscar(q, esAdmin && incluirInactivos));
    } catch (err) {
      setError(extraerMensajeError(err, "No se pudieron cargar los clientes"));
    } finally {
      setCargando(false);
    }
  }

  useEffect(() => {
    cargar(busqueda, mostrarInactivos);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [mostrarInactivos]);

  function abrirNuevo() {
    setEditandoRuc(null);
    setForm(FORM_VACIO);
    setErrorFormulario(null);
    setEstadoRuc({ validando: false, mensaje: null, valido: true });
    setMostrarFormulario(true);
  }

  function abrirEdicion(cliente: Cliente) {
    setEditandoRuc(cliente.ruc);
    setForm({
      ruc: cliente.ruc,
      razonSocial: cliente.razonSocial,
      direccion: cliente.direccion ?? "",
      email: cliente.email ?? "",
      condicionIva: cliente.condicionIva,
    });
    setErrorFormulario(null);
    setEstadoRuc({ validando: false, mensaje: null, valido: true });
    setMostrarFormulario(true);
  }

  async function onValidarRuc() {
    if (editandoRuc || !form.ruc.trim()) return;
    setEstadoRuc({ validando: true, mensaje: null, valido: true });
    try {
      const resultado = await clientesApi.validarRuc(form.ruc.trim());
      setEstadoRuc({ validando: false, mensaje: resultado.mensaje, valido: resultado.valido });
    } catch (err) {
      setEstadoRuc({
        validando: false,
        mensaje: extraerMensajeError(err, "No se pudo validar el RUC"),
        valido: false,
      });
    }
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setErrorFormulario(null);
    try {
      if (editandoRuc) {
        await clientesApi.editar(editandoRuc, form);
      } else {
        await clientesApi.crear(form);
      }
      setMostrarFormulario(false);
      await cargar(busqueda);
    } catch (err) {
      setErrorFormulario(extraerMensajeError(err, "No se pudo guardar el cliente"));
    }
  }

  async function onDesactivar(cliente: Cliente) {
    if (!confirm(`¿Desactivar al cliente ${cliente.razonSocial}?`)) return;
    try {
      await clientesApi.desactivar(cliente.ruc);
      await cargar(busqueda);
    } catch (err) {
      setError(extraerMensajeError(err, "No se pudo desactivar el cliente"));
    }
  }

  return (
    <div>
      <div className="page-header">
        <h1>Gestión de clientes</h1>
        {esAdmin && <button onClick={abrirNuevo}>+ Nuevo cliente</button>}
      </div>

      <div className="toolbar">
        <input
          placeholder="Buscar por RUC, razón social o email…"
          value={busqueda}
          onChange={(e) => setBusqueda(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && cargar(busqueda)}
        />
        <button className="secondary" onClick={() => cargar(busqueda)}>
          Buscar
        </button>
        {esAdmin && (
          <label className="checkbox-inline">
            <input
              type="checkbox"
              checked={mostrarInactivos}
              onChange={(e) => setMostrarInactivos(e.target.checked)}
            />
            Mostrar inactivos
          </label>
        )}
      </div>

      {error && <p className="error-text">{error}</p>}
      {cargando && <p>Cargando…</p>}

      <table className="data-table">
        <thead>
          <tr>
            <th>RUC/CI</th>
            <th>Razón social</th>
            <th>Email</th>
            <th>Condición IVA</th>
            <th>Estado</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {clientes.map((c) => (
            <tr key={c.ruc}>
              <td>{c.ruc}</td>
              <td>{c.razonSocial}</td>
              <td>{c.email}</td>
              <td>{CONDICION_IVA_LABEL[c.condicionIva]}</td>
              <td>
                <span className={`badge ${c.activo ? "badge-ok" : "badge-off"}`}>
                  {c.activo ? "Activo" : "Inactivo"}
                </span>
              </td>
              <td className="actions">
                {esAdmin ? (
                  <>
                    <button className="link-button" onClick={() => abrirEdicion(c)}>
                      Editar
                    </button>
                    {c.activo && (
                      <button className="link-button danger" onClick={() => onDesactivar(c)}>
                        Desactivar
                      </button>
                    )}
                  </>
                ) : (
                  <span className="hint-text">Solo lectura</span>
                )}
              </td>
            </tr>
          ))}
          {!cargando && clientes.length === 0 && (
            <tr>
              <td colSpan={6} className="empty-state">
                No hay clientes para mostrar.
              </td>
            </tr>
          )}
        </tbody>
      </table>

      {mostrarFormulario && (
        <div className="modal-backdrop" onClick={() => setMostrarFormulario(false)}>
          <form className="modal" onClick={(e) => e.stopPropagation()} onSubmit={onSubmit}>
            <h2>{editandoRuc ? "Editar cliente" : "Nuevo cliente"}</h2>

            <label>RUC / CI</label>
            <input
              value={form.ruc}
              disabled={!!editandoRuc}
              onChange={(e) => setForm({ ...form, ruc: e.target.value })}
              onBlur={onValidarRuc}
              placeholder="80012345-0"
              required
            />
            {estadoRuc.validando && <p className="hint-text">Validando RUC…</p>}
            {!estadoRuc.validando && estadoRuc.mensaje && (
              <p className={estadoRuc.valido ? "hint-text success" : "error-text"}>{estadoRuc.mensaje}</p>
            )}

            <label>Razón social</label>
            <input
              value={form.razonSocial}
              onChange={(e) => setForm({ ...form, razonSocial: e.target.value })}
              required
            />

            <label>Dirección</label>
            <input
              value={form.direccion}
              onChange={(e) => setForm({ ...form, direccion: e.target.value })}
            />

            <label>Email</label>
            <input
              type="email"
              value={form.email}
              onChange={(e) => setForm({ ...form, email: e.target.value })}
            />

            <label>Condición ante el IVA</label>
            <select
              value={form.condicionIva}
              onChange={(e) => setForm({ ...form, condicionIva: e.target.value as CondicionIva })}
            >
              {Object.entries(CONDICION_IVA_LABEL).map(([valor, etiqueta]) => (
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
