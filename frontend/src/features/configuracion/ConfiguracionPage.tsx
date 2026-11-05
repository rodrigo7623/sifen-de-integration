import { Plus, Star, X } from "lucide-react";
import { useEffect, useState, type ChangeEvent, type FormEvent, type InputHTMLAttributes } from "react";
import { extraerMensajeError } from "../../api/client";
import type { ActividadEconomica, ConfiguracionEmisor } from "../../api/types";
import { useAuth } from "../../auth/AuthContext";
import { Button } from "../../components/ui/Button";
import { FormField } from "../../components/ui/FormField";
import { Skeleton } from "../../components/ui/Skeleton";
import { useToast } from "../../components/ui/ToastProvider";
import { configuracionApi } from "./api";

function aTexto(valor: string | number | null): string {
  return valor === null || valor === undefined ? "" : String(valor);
}

/** Máximo de actividades del grupo D2.1 (gActEco) del Manual Técnico v150. */
const MAX_ACTIVIDADES = 9;

/** Validación en pantalla según el tipo de cada columna de configuracion_emisor. */
const NUMERICO = (max: number) => ({ inputMode: "numeric" as const, pattern: `\\d{0,${max}}`, maxLength: max });

export function ConfiguracionPage() {
  const { usuario } = useAuth();
  const esAdmin = usuario?.rol === "ADMIN";
  const { showToast } = useToast();
  const [form, setForm] = useState<ConfiguracionEmisor | null>(null);
  const [cargando, setCargando] = useState(true);
  const [guardando, setGuardando] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    configuracionApi
      .obtener()
      .then(setForm)
      .catch((err) => setError(extraerMensajeError(err, "No se pudo cargar la configuración")))
      .finally(() => setCargando(false));
  }, []);

  function campo(
    nombre: keyof ConfiguracionEmisor,
    extra: Pick<InputHTMLAttributes<HTMLInputElement>, "maxLength" | "pattern" | "inputMode" | "title"> = {},
  ) {
    return {
      ...extra,
      value: form ? aTexto(form[nombre] as string | number | null) : "",
      disabled: !esAdmin,
      onChange: (e: ChangeEvent<HTMLInputElement | HTMLSelectElement>) =>
        setForm((prev) => (prev ? { ...prev, [nombre]: e.target.value || null } : prev)),
    };
  }

  const actividades = form?.actividadesEconomicas ?? [];

  function cambiarActividades(nuevas: ActividadEconomica[]) {
    setForm((prev) => (prev ? { ...prev, actividadesEconomicas: nuevas } : prev));
  }

  function editarActividad(indice: number, cambios: Partial<ActividadEconomica>) {
    cambiarActividades(actividades.map((a, i) => (i === indice ? { ...a, ...cambios } : a)));
  }

  function marcarPrincipal(indice: number) {
    cambiarActividades(actividades.map((a, i) => ({ ...a, principal: i === indice })));
  }

  function agregarActividad() {
    cambiarActividades([...actividades, { codigo: "", descripcion: "", principal: actividades.length === 0 }]);
  }

  function quitarActividad(indice: number) {
    const quedan = actividades.filter((_, i) => i !== indice);
    // Si se quitó la principal, pasa a serlo la primera que quede.
    if (quedan.length > 0 && !quedan.some((a) => a.principal)) quedan[0] = { ...quedan[0], principal: true };
    cambiarActividades(quedan);
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    if (!form) return;
    setError(null);
    if (actividades.length > 0 && actividades.filter((a) => a.principal).length !== 1) {
      setError("Marcá exactamente una actividad económica como principal");
      return;
    }
    setGuardando(true);
    try {
      const actualizado = await configuracionApi.actualizar({
        ...form,
        dvRuc: form.dvRuc !== null ? Number(form.dvRuc) : null,
        tipoContribuyente: form.tipoContribuyente !== null ? Number(form.tipoContribuyente) : null,
        actividadesEconomicas: actividades.map((a) => ({ ...a, codigo: a.codigo.trim(), descripcion: a.descripcion.trim() })),
      });
      setForm(actualizado);
      showToast("Configuración guardada", "success");
    } catch (err) {
      setError(extraerMensajeError(err, "No se pudo guardar la configuración"));
    } finally {
      setGuardando(false);
    }
  }

  if (cargando) {
    return (
      <div>
        <h1 className="mb-4 text-xl font-semibold">Configuración del emisor</h1>
        <Skeleton rows={6} />
      </div>
    );
  }

  if (!form) {
    return <p className="text-sm text-rojo">{error ?? "Sin datos"}</p>;
  }

  return (
    <div>
      <h1 className="mb-2 text-xl font-semibold">Configuración del emisor</h1>
      <p className="mb-4 max-w-2xl text-sm text-texto-suave">
        Razón social, dirección, actividades económicas y timbrado de Tottal Store ante la SET. Son los datos del
        emisor que van en cada documento enviado al SIFEN y en el KuDE.
        {!esAdmin && " Solo un administrador puede editar estos datos."}
      </p>

      <form onSubmit={onSubmit} className="max-w-3xl">
        <div className="mb-4 rounded-lg border border-borde bg-white p-5">
          <h2 className="mb-3 text-sm font-semibold text-azul-oscuro">Identificación</h2>
          <div className="grid grid-cols-1 gap-x-4 sm:grid-cols-2">
            <FormField label="Razón social">
              <input {...campo("razonSocial", { maxLength: 255 })} />
            </FormField>
            <FormField label="Nombre de fantasía">
              <input {...campo("nombreFantasia", { maxLength: 255 })} />
            </FormField>
            <FormField label="RUC (base, sin dígito verificador)">
              <input {...campo("rucBase", { ...NUMERICO(8), title: "Solo números, hasta 8 dígitos" })} placeholder="4309652" />
            </FormField>
            <FormField label="Dígito verificador">
              <input type="number" min={0} max={9} {...campo("dvRuc")} placeholder="2" />
            </FormField>
            <FormField label="Tipo de contribuyente">
              <select {...campo("tipoContribuyente")}>
                <option value="">Seleccionar…</option>
                <option value="1">1 — Persona física</option>
                <option value="2">2 — Persona jurídica</option>
              </select>
            </FormField>
          </div>
        </div>

        <div className="mb-4 rounded-lg border border-borde bg-white p-5">
          <h2 className="mb-1 text-sm font-semibold text-azul-oscuro">Dirección</h2>
          <p className="mb-3 text-xs text-texto-suave">
            Departamento, distrito y ciudad llevan el <b>código numérico</b> de la tabla de referencia del SIFEN; el
            nombre va en la descripción.
          </p>
          <div className="grid grid-cols-1 gap-x-4 sm:grid-cols-2">
            <FormField label="Dirección">
              <input {...campo("direccion", { maxLength: 255 })} />
            </FormField>
            <FormField label="Número de casa">
              <input {...campo("numeroCasa", { maxLength: 10 })} />
            </FormField>
            <FormField label="Departamento (código)">
              <input {...campo("departamentoCodigo", { ...NUMERICO(5), title: "Código numérico, hasta 5 dígitos" })} placeholder="1" />
            </FormField>
            <FormField label="Departamento (descripción)">
              <input {...campo("departamentoDescripcion", { maxLength: 50 })} placeholder="CAPITAL" />
            </FormField>
            <FormField label="Distrito (código, opcional)">
              <input {...campo("distritoCodigo", { ...NUMERICO(5), title: "Código numérico, hasta 5 dígitos" })} />
            </FormField>
            <FormField label="Distrito (descripción)">
              <input {...campo("distritoDescripcion", { maxLength: 50 })} />
            </FormField>
            <FormField label="Ciudad (código)">
              <input {...campo("ciudadCodigo", { ...NUMERICO(5), title: "Código numérico, hasta 5 dígitos" })} placeholder="1" />
            </FormField>
            <FormField label="Ciudad (descripción)">
              <input {...campo("ciudadDescripcion", { maxLength: 50 })} placeholder="ASUNCION (DISTRITO)" />
            </FormField>
            <FormField label="Teléfono">
              <input {...campo("telefono", { maxLength: 20 })} />
            </FormField>
            <FormField label="Email">
              <input type="email" {...campo("email", { maxLength: 100 })} />
            </FormField>
          </div>
        </div>

        <div className="mb-4 rounded-lg border border-borde bg-white p-5">
          <div className="mb-1 flex items-center justify-between gap-2">
            <h2 className="m-0 text-sm font-semibold text-azul-oscuro">Actividades económicas</h2>
            {esAdmin && actividades.length < MAX_ACTIVIDADES && (
              <Button type="button" variant="secondary" icon={<Plus size={16} />} onClick={agregarActividad}>
                Agregar actividad
              </Button>
            )}
          </div>
          <p className="mb-3 text-xs text-texto-suave">
            Las registradas ante la SET (hasta {MAX_ACTIVIDADES}). El SIFEN valida el código y el texto exactos. La{" "}
            <b>principal</b> se muestra en el KuDE y va primera en el documento electrónico.
          </p>
          {actividades.length === 0 && (
            <p className="m-0 text-sm text-texto-suave">Sin actividades cargadas: el SIFEN exige al menos una.</p>
          )}
          {actividades.map((a, i) => (
            <div key={i} className="mb-2 flex flex-wrap items-center gap-2 sm:flex-nowrap">
              <label
                className={`flex shrink-0 items-center gap-1.5 whitespace-nowrap rounded-md border px-2 py-2 text-sm ${a.principal ? "border-azul bg-azul-claro text-azul-oscuro" : "border-borde text-texto-suave"}`}
                title="Actividad principal"
              >
                <input
                  type="radio"
                  name="actividadPrincipal"
                  className="w-auto"
                  checked={a.principal}
                  disabled={!esAdmin}
                  onChange={() => marcarPrincipal(i)}
                />
                <Star size={14} className={a.principal ? "fill-current" : ""} />
                Principal
              </label>
              <input
                className="sm:w-32"
                placeholder="Código"
                aria-label="Código de la actividad"
                value={a.codigo}
                maxLength={10}
                required
                disabled={!esAdmin}
                onChange={(e) => editarActividad(i, { codigo: e.target.value })}
              />
              <input
                placeholder="Descripción exacta, como figura en la SET"
                aria-label="Descripción de la actividad"
                value={a.descripcion}
                maxLength={300}
                required
                disabled={!esAdmin}
                onChange={(e) => editarActividad(i, { descripcion: e.target.value })}
              />
              {esAdmin && (
                <button
                  type="button"
                  className="shrink-0 rounded-md p-1.5 text-rojo hover:bg-rojo-claro"
                  onClick={() => quitarActividad(i)}
                  aria-label="Quitar actividad"
                  title="Quitar actividad"
                >
                  <X size={16} />
                </button>
              )}
            </div>
          ))}
        </div>

        <div className="mb-4 rounded-lg border border-borde bg-white p-5">
          <h2 className="mb-3 text-sm font-semibold text-azul-oscuro">Timbrado</h2>
          <p className="mb-3 text-sm text-texto-suave">
            Los establecimientos y puntos de expedición se gestionan en su propia pantalla (menú "Establecimientos"):
            cada factura elige desde cuál se emite.
          </p>
          <div className="grid grid-cols-1 gap-x-4 sm:grid-cols-2">
            <FormField label="Número de timbrado">
              <input {...campo("timbradoNumero", { ...NUMERICO(8), title: "Solo números, 8 dígitos" })} placeholder="04309652" />
            </FormField>
            <FormField label="Vigencia desde">
              <input type="date" {...campo("timbradoFechaInicio")} />
            </FormField>
            <FormField label="Vigencia hasta">
              <input type="date" {...campo("timbradoFechaFin")} />
            </FormField>
          </div>
        </div>

        {error && <p className="mb-3 text-sm text-rojo">{error}</p>}

        {esAdmin && (
          <div className="flex justify-end">
            <Button type="submit" disabled={guardando}>
              {guardando ? "Guardando…" : "Guardar configuración"}
            </Button>
          </div>
        )}
      </form>
    </div>
  );
}
