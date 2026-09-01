import { useEffect, useState, type FormEvent } from "react";
import { extraerMensajeError } from "../../api/client";
import { useAuth } from "../../auth/AuthContext";
import { ROL_LABEL, type Rol, type Usuario } from "../../api/types";
import { usuariosApi, type UsuarioCrearInput, type UsuarioEditarInput } from "./api";

const FORM_VACIO: UsuarioCrearInput = {
  nombre: "",
  email: "",
  password: "",
  rol: "OPERARIO",
};

export function UsuariosPage() {
  const { usuario: usuarioActual } = useAuth();
  const [usuarios, setUsuarios] = useState<Usuario[]>([]);
  const [busqueda, setBusqueda] = useState("");
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [mostrarFormulario, setMostrarFormulario] = useState(false);
  const [editandoId, setEditandoId] = useState<string | null>(null);
  const [form, setForm] = useState<UsuarioCrearInput>(FORM_VACIO);
  const [errorFormulario, setErrorFormulario] = useState<string | null>(null);

  async function cargar(q?: string) {
    setCargando(true);
    setError(null);
    try {
      setUsuarios(await usuariosApi.buscar(q));
    } catch (err) {
      setError(extraerMensajeError(err, "No se pudieron cargar los usuarios"));
    } finally {
      setCargando(false);
    }
  }

  useEffect(() => {
    cargar();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function abrirNuevo() {
    setEditandoId(null);
    setForm(FORM_VACIO);
    setErrorFormulario(null);
    setMostrarFormulario(true);
  }

  function abrirEdicion(u: Usuario) {
    setEditandoId(u.id);
    setForm({ nombre: u.nombre, email: u.email, password: "", rol: u.rol });
    setErrorFormulario(null);
    setMostrarFormulario(true);
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setErrorFormulario(null);
    try {
      if (editandoId) {
        const input: UsuarioEditarInput = {
          nombre: form.nombre,
          rol: form.rol,
          password: form.password.trim() === "" ? null : form.password,
        };
        await usuariosApi.editar(editandoId, input);
      } else {
        await usuariosApi.crear(form);
      }
      setMostrarFormulario(false);
      await cargar(busqueda);
    } catch (err) {
      setErrorFormulario(extraerMensajeError(err, "No se pudo guardar el usuario"));
    }
  }

  async function onCambiarEstado(u: Usuario) {
    const accion = u.activo ? "Desactivar" : "Activar";
    if (!confirm(`¿${accion} al usuario ${u.nombre}?`)) return;
    try {
      if (u.activo) {
        await usuariosApi.desactivar(u.id);
      } else {
        await usuariosApi.activar(u.id);
      }
      await cargar(busqueda);
    } catch (err) {
      setError(extraerMensajeError(err, `No se pudo ${accion.toLowerCase()} el usuario`));
    }
  }

  return (
    <div>
      <div className="page-header">
        <h1>Usuarios</h1>
        <button onClick={abrirNuevo}>+ Nuevo usuario</button>
      </div>

      <div className="toolbar">
        <input
          placeholder="Buscar por nombre o email…"
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
            <th>Nombre</th>
            <th>Email</th>
            <th>Rol</th>
            <th>Estado</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {usuarios.map((u) => (
            <tr key={u.id}>
              <td>{u.nombre}</td>
              <td>{u.email}</td>
              <td>{ROL_LABEL[u.rol]}</td>
              <td>
                <span className={`badge ${u.activo ? "badge-ok" : "badge-off"}`}>
                  {u.activo ? "Activo" : "Inactivo"}
                </span>
              </td>
              <td className="actions">
                <button className="link-button" onClick={() => abrirEdicion(u)}>
                  Editar
                </button>
                {u.email !== usuarioActual?.email ? (
                  <button
                    className={`link-button ${u.activo ? "danger" : ""}`}
                    onClick={() => onCambiarEstado(u)}
                  >
                    {u.activo ? "Desactivar" : "Activar"}
                  </button>
                ) : (
                  <span className="hint-text">(vos)</span>
                )}
              </td>
            </tr>
          ))}
          {!cargando && usuarios.length === 0 && (
            <tr>
              <td colSpan={5} className="empty-state">
                No hay usuarios para mostrar.
              </td>
            </tr>
          )}
        </tbody>
      </table>

      {mostrarFormulario && (
        <div className="modal-backdrop" onClick={() => setMostrarFormulario(false)}>
          <form className="modal" onClick={(e) => e.stopPropagation()} onSubmit={onSubmit}>
            <h2>{editandoId ? "Editar usuario" : "Nuevo usuario"}</h2>

            <label>Nombre</label>
            <input
              value={form.nombre}
              onChange={(e) => setForm({ ...form, nombre: e.target.value })}
              required
            />

            <label>Email</label>
            <input
              type="email"
              value={form.email}
              disabled={!!editandoId}
              onChange={(e) => setForm({ ...form, email: e.target.value })}
              required
            />

            <label>{editandoId ? "Nueva contraseña (dejar en blanco para no cambiarla)" : "Contraseña"}</label>
            <input
              type="password"
              value={form.password}
              onChange={(e) => setForm({ ...form, password: e.target.value })}
              minLength={6}
              required={!editandoId}
            />

            <label>Rol</label>
            <select value={form.rol} onChange={(e) => setForm({ ...form, rol: e.target.value as Rol })}>
              {Object.entries(ROL_LABEL).map(([valor, etiqueta]) => (
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
