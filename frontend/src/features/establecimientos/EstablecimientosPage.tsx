import { Plus } from "lucide-react";
import { useEffect, useState, type ChangeEvent, type FormEvent, type InputHTMLAttributes } from "react";
import { extraerMensajeError } from "../../api/client";
import type { Establecimiento, PuntoExpedicion } from "../../api/types";
import { Badge } from "../../components/ui/Badge";
import { Button } from "../../components/ui/Button";
import { useConfirm } from "../../components/ui/ConfirmProvider";
import { EmptyState } from "../../components/ui/EmptyState";
import { FormField } from "../../components/ui/FormField";
import { Modal } from "../../components/ui/Modal";
import { Skeleton } from "../../components/ui/Skeleton";
import { useToast } from "../../components/ui/ToastProvider";
import {
  establecimientosApi,
  type EstablecimientoInput,
  type PuntoExpedicionInput,
} from "./api";

const FORM_ESTABLECIMIENTO_VACIO: EstablecimientoInput = {
  codigo: "",
  denominacion: "",
  direccion: "",
  numeroCasa: "",
  departamentoCodigo: "",
  departamentoDescripcion: "",
  distritoCodigo: "",
  distritoDescripcion: "",
  ciudadCodigo: "",
  ciudadDescripcion: "",
  telefono: "",
  email: "",
  activo: true,
};

const FORM_PUNTO_VACIO: PuntoExpedicionInput = { codigo: "", descripcion: "" };

export function EstablecimientosPage() {
  const confirmar = useConfirm();
  const { showToast } = useToast();
  const [establecimientos, setEstablecimientos] = useState<Establecimiento[]>([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [modalEstablecimiento, setModalEstablecimiento] = useState(false);
  const [editandoId, setEditandoId] = useState<string | null>(null);
  const [formEstablecimiento, setFormEstablecimiento] = useState(FORM_ESTABLECIMIENTO_VACIO);
  const [errorFormulario, setErrorFormulario] = useState<string | null>(null);

  const [modalPunto, setModalPunto] = useState<{ establecimientoId: string; puntoId: string | null } | null>(
    null,
  );
  const [formPunto, setFormPunto] = useState(FORM_PUNTO_VACIO);

  async function cargar() {
    setCargando(true);
    setError(null);
    try {
      setEstablecimientos(await establecimientosApi.listar(true));
    } catch (err) {
      setError(extraerMensajeError(err, "No se pudieron cargar los establecimientos"));
    } finally {
      setCargando(false);
    }
  }

  useEffect(() => {
    cargar();
  }, []);

  function abrirNuevoEstablecimiento() {
    setEditandoId(null);
    setFormEstablecimiento(FORM_ESTABLECIMIENTO_VACIO);
    setErrorFormulario(null);
    setModalEstablecimiento(true);
  }

  function abrirEdicionEstablecimiento(e: Establecimiento) {
    setEditandoId(e.id);
    setFormEstablecimiento({
      codigo: e.codigo,
      denominacion: e.denominacion,
      direccion: e.direccion ?? "",
      numeroCasa: e.numeroCasa ?? "",
      departamentoCodigo: e.departamentoCodigo ?? "",
      departamentoDescripcion: e.departamentoDescripcion ?? "",
      distritoCodigo: e.distritoCodigo ?? "",
      distritoDescripcion: e.distritoDescripcion ?? "",
      ciudadCodigo: e.ciudadCodigo ?? "",
      ciudadDescripcion: e.ciudadDescripcion ?? "",
      telefono: e.telefono ?? "",
      email: e.email ?? "",
      activo: e.activo,
    });
    setErrorFormulario(null);
    setModalEstablecimiento(true);
  }

  function campoEstablecimiento(nombre: keyof Omit<EstablecimientoInput, "activo">, extra: InputHTMLAttributes<HTMLInputElement>) {
    return {
      ...extra,
      value: formEstablecimiento[nombre],
      onChange: (e: ChangeEvent<HTMLInputElement>) =>
        setFormEstablecimiento({ ...formEstablecimiento, [nombre]: e.target.value }),
    };
  }

  async function onSubmitEstablecimiento(ev: FormEvent) {
    ev.preventDefault();
    setErrorFormulario(null);
    try {
      if (editandoId) {
        await establecimientosApi.editar(editandoId, formEstablecimiento);
      } else {
        await establecimientosApi.crear(formEstablecimiento);
      }
      setModalEstablecimiento(false);
      showToast(editandoId ? "Establecimiento actualizado" : "Establecimiento creado", "success");
      await cargar();
    } catch (err) {
      setErrorFormulario(extraerMensajeError(err, "No se pudo guardar el establecimiento"));
    }
  }

  async function onCambiarEstadoEstablecimiento(e: Establecimiento) {
    const accion = e.activo ? "Desactivar" : "Activar";
    if (!(await confirmar(`¿${accion} el establecimiento ${e.codigo} - ${e.denominacion}?`))) return;
    try {
      e.activo ? await establecimientosApi.desactivar(e.id) : await establecimientosApi.activar(e.id);
      showToast(`Establecimiento ${e.activo ? "desactivado" : "activado"}`, "success");
      await cargar();
    } catch (err) {
      showToast(extraerMensajeError(err, `No se pudo ${accion.toLowerCase()} el establecimiento`), "error");
    }
  }

  function abrirNuevoPunto(establecimientoId: string) {
    setModalPunto({ establecimientoId, puntoId: null });
    setFormPunto(FORM_PUNTO_VACIO);
    setErrorFormulario(null);
  }

  function abrirEdicionPunto(establecimientoId: string, p: PuntoExpedicion) {
    setModalPunto({ establecimientoId, puntoId: p.id });
    setFormPunto({ codigo: p.codigo, descripcion: p.descripcion ?? "" });
    setErrorFormulario(null);
  }

  async function onSubmitPunto(ev: FormEvent) {
    ev.preventDefault();
    if (!modalPunto) return;
    setErrorFormulario(null);
    try {
      if (modalPunto.puntoId) {
        await establecimientosApi.editarPunto(modalPunto.establecimientoId, modalPunto.puntoId, formPunto);
      } else {
        await establecimientosApi.agregarPunto(modalPunto.establecimientoId, formPunto);
      }
      setModalPunto(null);
      showToast(modalPunto.puntoId ? "Punto de expedición actualizado" : "Punto de expedición creado", "success");
      await cargar();
    } catch (err) {
      setErrorFormulario(extraerMensajeError(err, "No se pudo guardar el punto de expedición"));
    }
  }

  async function onCambiarEstadoPunto(establecimientoId: string, p: PuntoExpedicion) {
    const accion = p.activo ? "Desactivar" : "Activar";
    if (!(await confirmar(`¿${accion} el punto de expedición ${p.codigo}?`))) return;
    try {
      p.activo
        ? await establecimientosApi.desactivarPunto(establecimientoId, p.id)
        : await establecimientosApi.activarPunto(establecimientoId, p.id);
      showToast(`Punto de expedición ${p.activo ? "desactivado" : "activado"}`, "success");
      await cargar();
    } catch (err) {
      showToast(extraerMensajeError(err, `No se pudo ${accion.toLowerCase()} el punto de expedición`), "error");
    }
  }

  return (
    <div>
      <div className="mb-2 flex items-center justify-between">
        <h1 className="m-0 text-xl font-semibold">Establecimientos</h1>
        <Button icon={<Plus size={16} />} onClick={abrirNuevoEstablecimiento}>
          Nuevo establecimiento
        </Button>
      </div>
      <p className="mb-4 max-w-2xl text-sm text-texto-suave">
        Cada establecimiento (local/sucursal) puede tener uno o más puntos de expedición (cajas). Al
        cargar una factura, el vendedor elige desde cuál se emite.
      </p>

      {error && <p className="mb-3 text-sm text-rojo">{error}</p>}

      {cargando ? (
        <Skeleton rows={4} />
      ) : establecimientos.length === 0 ? (
        <EmptyState message="No hay establecimientos cargados todavía." />
      ) : (
        <div className="flex flex-col gap-4">
          {establecimientos.map((e) => (
            <div key={e.id} className="rounded-lg bg-white p-5 shadow-card">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <div>
                  <span className="font-semibold">
                    {e.codigo} — {e.denominacion}
                  </span>{" "}
                  <Badge tone={e.activo ? "ok" : "off"}>{e.activo ? "Activo" : "Inactivo"}</Badge>
                  {e.direccion && (
                    <div className="mt-1 text-sm text-texto-suave">
                      {e.direccion} {e.numeroCasa}
                    </div>
                  )}
                </div>
                <div className="flex gap-1">
                  <Button variant="link" onClick={() => abrirEdicionEstablecimiento(e)}>
                    Editar
                  </Button>
                  <Button
                    variant={e.activo ? "link-danger" : "link"}
                    onClick={() => onCambiarEstadoEstablecimiento(e)}
                  >
                    {e.activo ? "Desactivar" : "Activar"}
                  </Button>
                </div>
              </div>

              <div className="mt-4">
                <div className="mb-2 flex items-center justify-between">
                  <span className="text-sm font-semibold text-azul-oscuro">Puntos de expedición</span>
                  <Button variant="secondary" icon={<Plus size={14} />} onClick={() => abrirNuevoPunto(e.id)}>
                    Punto de expedición
                  </Button>
                </div>
                {e.puntosExpedicion.length === 0 ? (
                  <p className="text-sm text-texto-suave">Todavía no tiene puntos de expedición.</p>
                ) : (
                  <table className="w-full border-collapse text-sm">
                    <tbody>
                      {e.puntosExpedicion.map((p) => (
                        <tr key={p.id} className="border-b border-borde last:border-0">
                          <td className="py-1.5 pr-3 font-medium">{p.codigo}</td>
                          <td className="py-1.5 pr-3 text-texto-suave">{p.descripcion}</td>
                          <td className="py-1.5 pr-3">
                            <Badge tone={p.activo ? "ok" : "off"}>{p.activo ? "Activo" : "Inactivo"}</Badge>
                          </td>
                          <td className="py-1.5 text-right">
                            <Button variant="link" onClick={() => abrirEdicionPunto(e.id, p)}>
                              Editar
                            </Button>
                            <Button
                              variant={p.activo ? "link-danger" : "link"}
                              onClick={() => onCambiarEstadoPunto(e.id, p)}
                            >
                              {p.activo ? "Desactivar" : "Activar"}
                            </Button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      {modalEstablecimiento && (
        <Modal
          title={editandoId ? "Editar establecimiento" : "Nuevo establecimiento"}
          onClose={() => setModalEstablecimiento(false)}
        >
          <form onSubmit={onSubmitEstablecimiento}>
            <p className="mb-3 text-xs text-texto-suave">
              En departamento, distrito y ciudad va el <b>código numérico</b> de la tabla de referencia del SIFEN; el
              nombre va en el campo de al lado.
            </p>
            <div className="grid grid-cols-1 gap-x-4 sm:grid-cols-2">
              <FormField label="Código (3 dígitos)">
                <input {...campoEstablecimiento("codigo", { required: true, inputMode: "numeric", pattern: "\d{3}", maxLength: 3, placeholder: "001", title: "3 dígitos, ej. 001" })} />
              </FormField>
              <FormField label="Denominación">
                <input {...campoEstablecimiento("denominacion", { required: true, maxLength: 100, placeholder: "Casa matriz" })} />
              </FormField>
              <FormField label="Dirección">
                <input {...campoEstablecimiento("direccion", { maxLength: 255 })} />
              </FormField>
              <FormField label="Número de casa">
                <input {...campoEstablecimiento("numeroCasa", { maxLength: 10 })} />
              </FormField>
              <FormField label="Departamento (código numérico)">
                <input {...campoEstablecimiento("departamentoCodigo", { inputMode: "numeric", pattern: "\d{0,5}", maxLength: 5, title: "Código numérico del SIFEN, hasta 5 dígitos", placeholder: "1" })} />
              </FormField>
              <FormField label="Departamento (nombre)">
                <input {...campoEstablecimiento("departamentoDescripcion", { maxLength: 50, placeholder: "CAPITAL" })} />
              </FormField>
              <FormField label="Distrito (código numérico, opcional)">
                <input {...campoEstablecimiento("distritoCodigo", { inputMode: "numeric", pattern: "\d{0,5}", maxLength: 5, title: "Código numérico del SIFEN, hasta 5 dígitos" })} />
              </FormField>
              <FormField label="Distrito (nombre)">
                <input {...campoEstablecimiento("distritoDescripcion", { maxLength: 50 })} />
              </FormField>
              <FormField label="Ciudad (código numérico)">
                <input {...campoEstablecimiento("ciudadCodigo", { inputMode: "numeric", pattern: "\d{0,5}", maxLength: 5, title: "Código numérico del SIFEN, hasta 5 dígitos", placeholder: "1" })} />
              </FormField>
              <FormField label="Ciudad (nombre)">
                <input {...campoEstablecimiento("ciudadDescripcion", { maxLength: 50, placeholder: "ASUNCION (DISTRITO)" })} />
              </FormField>
              <FormField label="Teléfono">
                <input {...campoEstablecimiento("telefono", { maxLength: 20, type: "tel" })} />
              </FormField>
              <FormField label="Email">
                <input {...campoEstablecimiento("email", { maxLength: 100, type: "email" })} />
              </FormField>
            </div>

            {editandoId && (
              <label className="mb-3 flex items-center gap-2 text-sm">
                <input
                  type="checkbox"
                  className="w-auto"
                  checked={formEstablecimiento.activo}
                  onChange={(e) => setFormEstablecimiento({ ...formEstablecimiento, activo: e.target.checked })}
                />
                Activo (disponible para emitir facturas)
              </label>
            )}

            {errorFormulario && <p className="mb-2 text-sm text-rojo">{errorFormulario}</p>}

            <div className="mt-5 flex justify-end gap-2">
              <Button type="button" variant="secondary" onClick={() => setModalEstablecimiento(false)}>
                Cancelar
              </Button>
              <Button type="submit">Guardar</Button>
            </div>
          </form>
        </Modal>
      )}

      {modalPunto && (
        <Modal
          title={modalPunto.puntoId ? "Editar punto de expedición" : "Nuevo punto de expedición"}
          onClose={() => setModalPunto(null)}
        >
          <form onSubmit={onSubmitPunto}>
            <FormField label="Código (3 dígitos)">
              <input
                value={formPunto.codigo}
                onChange={(e) => setFormPunto({ ...formPunto, codigo: e.target.value })}
                placeholder="001"
                inputMode="numeric"
                pattern="d{3}"
                title="3 dígitos, ej. 001"
                maxLength={3}
                required
              />
            </FormField>
            <FormField label="Descripción">
              <input
                value={formPunto.descripcion}
                onChange={(e) => setFormPunto({ ...formPunto, descripcion: e.target.value })}
                placeholder="Caja principal"
                maxLength={100}
              />
            </FormField>

            {errorFormulario && <p className="mb-2 text-sm text-rojo">{errorFormulario}</p>}

            <div className="mt-5 flex justify-end gap-2">
              <Button type="button" variant="secondary" onClick={() => setModalPunto(null)}>
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
