import { Plus } from "lucide-react";
import { useEffect, useState, type FormEvent } from "react";
import { extraerMensajeError } from "../../api/client";
import { ROL_LABEL, type Rol, type Usuario } from "../../api/types";
import { useAuth } from "../../auth/AuthContext";
import { Badge } from "../../components/ui/Badge";
import { Button } from "../../components/ui/Button";
import { useConfirm } from "../../components/ui/ConfirmProvider";
import { type Column, DataTable } from "../../components/ui/DataTable";
import { FormField } from "../../components/ui/FormField";
import { Modal } from "../../components/ui/Modal";
import { useToast } from "../../components/ui/ToastProvider";
import { usuariosApi, type UsuarioCrearInput, type UsuarioEditarInput } from "./api";

const FORM_VACIO: UsuarioCrearInput = {
  nombre: "",
  email: "",
  password: "",
  rol: "OPERARIO",
};

export function UsuariosPage() {
  const { usuario: usuarioActual } = useAuth();
  const confirmar = useConfirm();
  const { showToast } = useToast();
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
      showToast(editandoId ? "Usuario actualizado" : "Usuario creado", "success");
      await cargar(busqueda);
    } catch (err) {
      setErrorFormulario(extraerMensajeError(err, "No se pudo guardar el usuario"));
    }
  }

  async function onCambiarEstado(u: Usuario) {
    const accion = u.activo ? "Desactivar" : "Activar";
    if (!(await confirmar(`¿${accion} al usuario ${u.nombre}?`))) return;
    try {
      if (u.activo) {
        await usuariosApi.desactivar(u.id);
      } else {
        await usuariosApi.activar(u.id);
      }
      showToast(`Usuario ${u.activo ? "desactivado" : "activado"}`, "success");
      await cargar(busqueda);
    } catch (err) {
      showToast(extraerMensajeError(err, `No se pudo ${accion.toLowerCase()} el usuario`), "error");
    }
  }

  const columnas: Column<Usuario>[] = [
    { header: "Nombre", render: (u) => u.nombre },
    { header: "Email", render: (u) => u.email },
    { header: "Rol", render: (u) => ROL_LABEL[u.rol] },
    {
      header: "Estado",
      render: (u) => <Badge tone={u.activo ? "ok" : "off"}>{u.activo ? "Activo" : "Inactivo"}</Badge>,
    },
    {
      header: "",
      className: "whitespace-nowrap text-right",
      render: (u) => (
        <>
          <Button variant="link" onClick={() => abrirEdicion(u)}>
            Editar
          </Button>
          {u.email !== usuarioActual?.email ? (
            <Button variant={u.activo ? "link-danger" : "link"} onClick={() => onCambiarEstado(u)}>
              {u.activo ? "Desactivar" : "Activar"}
            </Button>
          ) : (
            <span className="text-sm text-texto-suave">(vos)</span>
          )}
        </>
      ),
    },
  ];

  return (
    <div>
      <div className="mb-4 flex items-center justify-between">
        <h1 className="m-0 text-xl font-semibold">Usuarios</h1>
        <Button icon={<Plus size={16} />} onClick={abrirNuevo}>
          Nuevo usuario
        </Button>
      </div>

      <div className="mb-4 flex max-w-xl flex-wrap items-center gap-2">
        <input
          className="min-w-[220px] flex-1"
          placeholder="Buscar por nombre o email…"
          value={busqueda}
          onChange={(e) => setBusqueda(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && cargar(busqueda)}
        />
        <Button variant="secondary" onClick={() => cargar(busqueda)}>
          Buscar
        </Button>
      </div>

      {error && <p className="mb-3 text-sm text-rojo">{error}</p>}

      <DataTable
        columns={columnas}
        data={usuarios}
        keyExtractor={(u) => u.id}
        loading={cargando}
        emptyMessage="No hay usuarios para mostrar."
      />

      {mostrarFormulario && (
        <Modal
          title={editandoId ? "Editar usuario" : "Nuevo usuario"}
          onClose={() => setMostrarFormulario(false)}
        >
          <form onSubmit={onSubmit}>
            <FormField label="Nombre">
              <input
                value={form.nombre}
                onChange={(e) => setForm({ ...form, nombre: e.target.value })}
                required
              />
            </FormField>

            <FormField label="Email">
              <input
                type="email"
                value={form.email}
                disabled={!!editandoId}
                onChange={(e) => setForm({ ...form, email: e.target.value })}
                required
              />
            </FormField>

            <FormField
              label={editandoId ? "Nueva contraseña (dejar en blanco para no cambiarla)" : "Contraseña"}
            >
              <input
                type="password"
                value={form.password}
                onChange={(e) => setForm({ ...form, password: e.target.value })}
                minLength={6}
                required={!editandoId}
              />
            </FormField>

            <FormField label="Rol">
              <select value={form.rol} onChange={(e) => setForm({ ...form, rol: e.target.value as Rol })}>
                {Object.entries(ROL_LABEL).map(([valor, etiqueta]) => (
                  <option key={valor} value={valor}>
                    {etiqueta}
                  </option>
                ))}
              </select>
            </FormField>

            {errorFormulario && <p className="mb-2 text-sm text-rojo">{errorFormulario}</p>}

            <div className="mt-5 flex justify-end gap-2">
              <Button type="button" variant="secondary" onClick={() => setMostrarFormulario(false)}>
                Cancelar
              </Button>
              <Button type="submit">Guardar</Button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  );
}
