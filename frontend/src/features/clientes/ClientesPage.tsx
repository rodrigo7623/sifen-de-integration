import { Plus } from "lucide-react";
import { useEffect, useState, type FormEvent } from "react";
import { extraerMensajeError } from "../../api/client";
import { CONDICION_IVA_LABEL, type Cliente, type CondicionIva } from "../../api/types";
import { useAuth } from "../../auth/AuthContext";
import { Badge } from "../../components/ui/Badge";
import { Button } from "../../components/ui/Button";
import { useConfirm } from "../../components/ui/ConfirmProvider";
import { type Column, DataTable } from "../../components/ui/DataTable";
import { FormField } from "../../components/ui/FormField";
import { Modal } from "../../components/ui/Modal";
import { useToast } from "../../components/ui/ToastProvider";
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
  const confirmar = useConfirm();
  const { showToast } = useToast();
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
      showToast(editandoRuc ? "Cliente actualizado" : "Cliente creado", "success");
      await cargar(busqueda);
    } catch (err) {
      setErrorFormulario(extraerMensajeError(err, "No se pudo guardar el cliente"));
    }
  }

  async function onDesactivar(cliente: Cliente) {
    if (!(await confirmar(`¿Desactivar al cliente ${cliente.razonSocial}?`))) return;
    try {
      await clientesApi.desactivar(cliente.ruc);
      showToast("Cliente desactivado", "success");
      await cargar(busqueda);
    } catch (err) {
      showToast(extraerMensajeError(err, "No se pudo desactivar el cliente"), "error");
    }
  }

  const columnas: Column<Cliente>[] = [
    { header: "RUC/CI", render: (c) => c.ruc },
    { header: "Razón social", render: (c) => c.razonSocial },
    { header: "Email", render: (c) => c.email },
    { header: "Condición IVA", render: (c) => CONDICION_IVA_LABEL[c.condicionIva] },
    {
      header: "Estado",
      render: (c) => <Badge tone={c.activo ? "ok" : "off"}>{c.activo ? "Activo" : "Inactivo"}</Badge>,
    },
    {
      header: "",
      className: "whitespace-nowrap text-right",
      render: (c) =>
        esAdmin ? (
          <>
            <Button variant="link" onClick={() => abrirEdicion(c)}>
              Editar
            </Button>
            {c.activo && (
              <Button variant="link-danger" onClick={() => onDesactivar(c)}>
                Desactivar
              </Button>
            )}
          </>
        ) : (
          <span className="text-sm text-texto-suave">Solo lectura</span>
        ),
    },
  ];

  return (
    <div>
      <div className="mb-4 flex items-center justify-between">
        <h1 className="m-0 text-xl font-semibold">Gestión de clientes</h1>
        {esAdmin && (
          <Button icon={<Plus size={16} />} onClick={abrirNuevo}>
            Nuevo cliente
          </Button>
        )}
      </div>

      <div className="mb-4 flex max-w-xl flex-wrap items-center gap-2">
        <input
          className="min-w-[220px] flex-1"
          placeholder="Buscar por RUC, razón social o email…"
          value={busqueda}
          onChange={(e) => setBusqueda(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && cargar(busqueda)}
        />
        <Button variant="secondary" onClick={() => cargar(busqueda)}>
          Buscar
        </Button>
        {esAdmin && (
          <label className="flex items-center gap-1.5 whitespace-nowrap text-sm text-texto-suave">
            <input
              type="checkbox"
              className="w-auto"
              checked={mostrarInactivos}
              onChange={(e) => setMostrarInactivos(e.target.checked)}
            />
            Mostrar inactivos
          </label>
        )}
      </div>

      {error && <p className="mb-3 text-sm text-rojo">{error}</p>}

      <DataTable
        columns={columnas}
        data={clientes}
        keyExtractor={(c) => c.ruc}
        loading={cargando}
        emptyMessage="No hay clientes para mostrar."
      />

      {mostrarFormulario && (
        <Modal
          title={editandoRuc ? "Editar cliente" : "Nuevo cliente"}
          onClose={() => setMostrarFormulario(false)}
        >
          <form onSubmit={onSubmit}>
            <FormField label="RUC / CI">
              <input
                value={form.ruc}
                disabled={!!editandoRuc}
                onChange={(e) => setForm({ ...form, ruc: e.target.value })}
                onBlur={onValidarRuc}
                placeholder="80012345-0"
                required
              />
              {estadoRuc.validando && <p className="mt-1 text-sm text-texto-suave">Validando RUC…</p>}
              {!estadoRuc.validando && estadoRuc.mensaje && (
                <p className={`mt-1 text-sm ${estadoRuc.valido ? "text-verde" : "text-rojo"}`}>
                  {estadoRuc.mensaje}
                </p>
              )}
            </FormField>

            <FormField label="Razón social">
              <input
                value={form.razonSocial}
                onChange={(e) => setForm({ ...form, razonSocial: e.target.value })}
                required
              />
            </FormField>

            <FormField label="Dirección">
              <input
                value={form.direccion}
                onChange={(e) => setForm({ ...form, direccion: e.target.value })}
              />
            </FormField>

            <FormField label="Email">
              <input
                type="email"
                value={form.email}
                onChange={(e) => setForm({ ...form, email: e.target.value })}
              />
            </FormField>

            <FormField label="Condición ante el IVA">
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
