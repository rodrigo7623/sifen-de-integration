import { Plus } from "lucide-react";
import { useEffect, useState, type FormEvent } from "react";
import { extraerMensajeError } from "../../api/client";
import { TASA_IVA_LABEL, type Producto, type TasaIva } from "../../api/types";
import { useAuth } from "../../auth/AuthContext";
import { Badge } from "../../components/ui/Badge";
import { Button } from "../../components/ui/Button";
import { type Column, DataTable } from "../../components/ui/DataTable";
import { FormField } from "../../components/ui/FormField";
import { Modal } from "../../components/ui/Modal";
import { useConfirm } from "../../components/ui/ConfirmProvider";
import { useToast } from "../../components/ui/ToastProvider";
import { productosApi, type ProductoInput } from "./api";

const FORM_VACIO: ProductoInput = {
  codigo: "",
  descripcion: "",
  unidadMedida: "UN",
  precioBase: 0,
  tasaIva: "DIEZ",
};

export function ProductosPage() {
  const { usuario } = useAuth();
  const esAdmin = usuario?.rol === "ADMIN";
  const confirmar = useConfirm();
  const { showToast } = useToast();
  const [productos, setProductos] = useState<Producto[]>([]);
  const [busqueda, setBusqueda] = useState("");
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [mostrarFormulario, setMostrarFormulario] = useState(false);
  const [editandoCodigo, setEditandoCodigo] = useState<string | null>(null);
  const [form, setForm] = useState<ProductoInput>(FORM_VACIO);
  const [errorFormulario, setErrorFormulario] = useState<string | null>(null);
  const [mostrarInactivos, setMostrarInactivos] = useState(false);

  async function cargar(q?: string, incluirInactivos = mostrarInactivos) {
    setCargando(true);
    setError(null);
    try {
      setProductos(await productosApi.buscar(q, esAdmin && incluirInactivos));
    } catch (err) {
      setError(extraerMensajeError(err, "No se pudieron cargar los productos"));
    } finally {
      setCargando(false);
    }
  }

  useEffect(() => {
    cargar(busqueda, mostrarInactivos);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [mostrarInactivos]);

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
      showToast(editandoCodigo ? "Producto actualizado" : "Producto creado", "success");
      await cargar(busqueda);
    } catch (err) {
      setErrorFormulario(extraerMensajeError(err, "No se pudo guardar el producto"));
    }
  }

  async function onDesactivar(producto: Producto) {
    if (!(await confirmar(`¿Desactivar el producto ${producto.codigo}?`))) return;
    try {
      await productosApi.desactivar(producto.codigo);
      showToast("Producto desactivado", "success");
      await cargar(busqueda);
    } catch (err) {
      showToast(extraerMensajeError(err, "No se pudo desactivar el producto"), "error");
    }
  }

  const columnas: Column<Producto>[] = [
    { header: "Código", render: (p) => p.codigo },
    { header: "Descripción", render: (p) => p.descripcion },
    { header: "Unidad", render: (p) => p.unidadMedida },
    { header: "Precio base", render: (p) => p.precioBase.toLocaleString("es-PY") },
    { header: "Tasa IVA", render: (p) => TASA_IVA_LABEL[p.tasaIva] },
    {
      header: "Estado",
      render: (p) => <Badge tone={p.activo ? "ok" : "off"}>{p.activo ? "Activo" : "Inactivo"}</Badge>,
    },
    {
      header: "",
      className: "whitespace-nowrap text-right",
      render: (p) =>
        esAdmin ? (
          <>
            <Button variant="link" onClick={() => abrirEdicion(p)}>
              Editar
            </Button>
            {p.activo && (
              <Button variant="link-danger" onClick={() => onDesactivar(p)}>
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
        <h1 className="m-0 text-xl font-semibold">Catálogo de productos</h1>
        {esAdmin && (
          <Button icon={<Plus size={16} />} onClick={abrirNuevo}>
            Nuevo producto
          </Button>
        )}
      </div>

      <div className="mb-4 flex max-w-xl flex-wrap items-center gap-2">
        <input
          className="min-w-[220px] flex-1"
          placeholder="Buscar por código o descripción…"
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
        data={productos}
        keyExtractor={(p) => p.codigo}
        loading={cargando}
        emptyMessage="No hay productos para mostrar."
      />

      {mostrarFormulario && (
        <Modal
          title={editandoCodigo ? "Editar producto" : "Nuevo producto"}
          onClose={() => setMostrarFormulario(false)}
        >
          <form onSubmit={onSubmit}>
            <FormField label="Código">
              <input
                value={form.codigo}
                disabled={!!editandoCodigo}
                onChange={(e) => setForm({ ...form, codigo: e.target.value })}
                required
              />
            </FormField>

            <FormField label="Descripción">
              <input
                value={form.descripcion}
                onChange={(e) => setForm({ ...form, descripcion: e.target.value })}
                required
              />
            </FormField>

            <FormField label="Unidad de medida">
              <input
                value={form.unidadMedida}
                onChange={(e) => setForm({ ...form, unidadMedida: e.target.value })}
                required
              />
            </FormField>

            <FormField label="Precio base (Gs.)">
              <input
                type="number"
                min={0.01}
                step="0.01"
                value={form.precioBase}
                onChange={(e) => setForm({ ...form, precioBase: Number(e.target.value) })}
                required
              />
            </FormField>

            <FormField label="Tasa de IVA">
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
