import { useEffect, useState, type ChangeEvent, type FormEvent } from "react";
import { extraerMensajeError } from "../../api/client";
import type { ConfiguracionEmisor } from "../../api/types";
import { useAuth } from "../../auth/AuthContext";
import { Button } from "../../components/ui/Button";
import { FormField } from "../../components/ui/FormField";
import { Skeleton } from "../../components/ui/Skeleton";
import { useToast } from "../../components/ui/ToastProvider";
import { configuracionApi } from "./api";

function aTexto(valor: string | number | null): string {
  return valor === null || valor === undefined ? "" : String(valor);
}

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

  function campo(nombre: keyof ConfiguracionEmisor) {
    return {
      value: form ? aTexto(form[nombre] as string | number | null) : "",
      disabled: !esAdmin,
      onChange: (e: ChangeEvent<HTMLInputElement | HTMLSelectElement>) =>
        setForm((prev) => (prev ? { ...prev, [nombre]: e.target.value || null } : prev)),
    };
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    if (!form) return;
    setGuardando(true);
    setError(null);
    try {
      const actualizado = await configuracionApi.actualizar({
        ...form,
        dvRuc: form.dvRuc !== null ? Number(form.dvRuc) : null,
        tipoContribuyente: form.tipoContribuyente !== null ? Number(form.tipoContribuyente) : null,
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
        Razón social, dirección, actividad económica y timbrado de Tottal Store ante la SET.
        {!esAdmin && " Solo un administrador puede editar estos datos."} Esta información todavía no
        está conectada al envío real al SIFEN (eso se hace en una etapa posterior de la integración).
      </p>

      {error && <p className="mb-3 text-sm text-rojo">{error}</p>}

      <form onSubmit={onSubmit} className="max-w-2xl">
        <div className="mb-4 rounded-lg border border-borde bg-white p-5">
          <h2 className="mb-3 text-sm font-semibold text-azul-oscuro">Identificación</h2>
          <div className="grid grid-cols-1 gap-x-4 sm:grid-cols-2">
            <FormField label="Razón social">
              <input {...campo("razonSocial")} />
            </FormField>
            <FormField label="Nombre de fantasía">
              <input {...campo("nombreFantasia")} />
            </FormField>
            <FormField label="RUC (base)">
              <input {...campo("rucBase")} placeholder="4309652" />
            </FormField>
            <FormField label="Dígito verificador">
              <input type="number" {...campo("dvRuc")} placeholder="2" />
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
          <h2 className="mb-3 text-sm font-semibold text-azul-oscuro">Dirección</h2>
          <div className="grid grid-cols-1 gap-x-4 sm:grid-cols-2">
            <FormField label="Dirección">
              <input {...campo("direccion")} />
            </FormField>
            <FormField label="Número de casa">
              <input {...campo("numeroCasa")} />
            </FormField>
            <FormField label="Departamento (código)">
              <input {...campo("departamentoCodigo")} placeholder="11" />
            </FormField>
            <FormField label="Departamento (descripción)">
              <input {...campo("departamentoDescripcion")} placeholder="ASUNCIÓN" />
            </FormField>
            <FormField label="Ciudad (código)">
              <input {...campo("ciudadCodigo")} placeholder="1" />
            </FormField>
            <FormField label="Ciudad (descripción)">
              <input {...campo("ciudadDescripcion")} placeholder="ASUNCIÓN" />
            </FormField>
            <FormField label="Teléfono">
              <input {...campo("telefono")} />
            </FormField>
            <FormField label="Email">
              <input type="email" {...campo("email")} />
            </FormField>
          </div>
        </div>

        <div className="mb-4 rounded-lg border border-borde bg-white p-5">
          <h2 className="mb-3 text-sm font-semibold text-azul-oscuro">Actividad económica</h2>
          <div className="grid grid-cols-1 gap-x-4 sm:grid-cols-2">
            <FormField label="Código (CIIU)">
              <input {...campo("actividadEconomicaCodigo")} placeholder="47111" />
            </FormField>
            <FormField label="Descripción">
              <input {...campo("actividadEconomicaDescripcion")} placeholder="Venta al por menor" />
            </FormField>
          </div>
        </div>

        <div className="mb-4 rounded-lg border border-borde bg-white p-5">
          <h2 className="mb-3 text-sm font-semibold text-azul-oscuro">Timbrado</h2>
          <p className="mb-3 text-sm text-texto-suave">
            Los establecimientos y puntos de expedición ahora se gestionan en su propia pantalla
            (menú "Establecimientos") — cada factura elige desde cuál se emite.
          </p>
          <div className="grid grid-cols-1 gap-x-4 sm:grid-cols-2">
            <FormField label="Número de timbrado">
              <input {...campo("timbradoNumero")} placeholder="Pendiente de homologación" />
            </FormField>
            <FormField label="Vigencia desde">
              <input type="date" {...campo("timbradoFechaInicio")} />
            </FormField>
            <FormField label="Vigencia hasta">
              <input type="date" {...campo("timbradoFechaFin")} />
            </FormField>
          </div>
        </div>

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
