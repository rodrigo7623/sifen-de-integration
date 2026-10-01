import { CheckCircle2, FileText, TrendingUp, XCircle } from "lucide-react";
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { extraerMensajeError } from "../../api/client";
import type { ResumenDashboard } from "../../api/types";
import { Skeleton } from "../../components/ui/Skeleton";
import { dashboardApi } from "./api";

function Tarjeta({
  titulo,
  valor,
  icon: Icon,
  tono,
}: {
  titulo: string;
  valor: string;
  icon: typeof FileText;
  tono: "azul" | "verde" | "rojo" | "ambar";
}) {
  const tonoClasses: Record<string, string> = {
    azul: "bg-azul-claro text-azul-oscuro",
    verde: "bg-verde-claro text-verde",
    rojo: "bg-rojo-claro text-rojo",
    ambar: "bg-ambar-claro text-ambar",
  };
  return (
    <div className="flex items-center gap-3.5 rounded-lg bg-white p-5 shadow-card">
      <div className={`rounded-lg p-2.5 ${tonoClasses[tono]}`}>
        <Icon size={22} />
      </div>
      <div>
        <div className="text-xs text-texto-suave">{titulo}</div>
        <div className="text-xl font-semibold text-texto">{valor}</div>
      </div>
    </div>
  );
}

export function DashboardPage() {
  const navigate = useNavigate();
  const [resumen, setResumen] = useState<ResumenDashboard | null>(null);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    dashboardApi
      .resumen()
      .then(setResumen)
      .catch((err) => setError(extraerMensajeError(err, "No se pudo cargar el resumen")))
      .finally(() => setCargando(false));
  }, []);

  if (cargando) {
    return (
      <div>
        <h1 className="mb-4 text-xl font-semibold">Inicio</h1>
        <Skeleton rows={5} />
      </div>
    );
  }

  if (error || !resumen) {
    return <p className="text-sm text-rojo">{error ?? "Sin datos"}</p>;
  }

  const aprobadas = resumen.facturasPorEstado.APROBADO ?? 0;
  const rechazadas = resumen.facturasPorEstado.RECHAZADO ?? 0;
  const borradores = resumen.facturasPorEstado.BORRADOR ?? 0;

  return (
    <div>
      <h1 className="mb-4 text-xl font-semibold">Inicio</h1>

      <div className="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <Tarjeta
          titulo="Facturado este mes (aprobadas)"
          valor={`Gs. ${resumen.totalFacturadoMes.toLocaleString("es-PY")}`}
          icon={TrendingUp}
          tono="azul"
        />
        <Tarjeta titulo="Facturas aprobadas" valor={String(aprobadas)} icon={CheckCircle2} tono="verde" />
        <Tarjeta titulo="Facturas rechazadas" valor={String(rechazadas)} icon={XCircle} tono="rojo" />
        <Tarjeta titulo="Borradores pendientes" valor={String(borradores)} icon={FileText} tono="ambar" />
      </div>

      <h2 className="mb-2 text-base font-semibold text-azul-oscuro">Últimos rechazos</h2>
      <div className="overflow-x-auto rounded-lg bg-white shadow-card">
        <table className="w-full min-w-[480px] border-collapse text-sm">
          <thead>
            <tr>
              <th className="bg-azul-claro px-3.5 py-2.5 text-left font-semibold text-azul-oscuro">
                Fecha
              </th>
              <th className="bg-azul-claro px-3.5 py-2.5 text-left font-semibold text-azul-oscuro">
                Cliente
              </th>
              <th className="bg-azul-claro px-3.5 py-2.5 text-left font-semibold text-azul-oscuro">
                Motivo
              </th>
            </tr>
          </thead>
          <tbody>
            {resumen.ultimosRechazados.length === 0 ? (
              <tr>
                <td colSpan={3} className="py-6 text-center text-sm text-texto-suave">
                  Sin facturas rechazadas.
                </td>
              </tr>
            ) : (
              resumen.ultimosRechazados.map((r) => (
                <tr
                  key={r.facturaId}
                  className="cursor-pointer border-b border-borde last:border-0 hover:bg-azul-claro/40"
                  onClick={() => navigate(`/facturas/${r.facturaId}`)}
                >
                  <td className="px-3.5 py-2.5">{new Date(r.fechaEmision).toLocaleDateString("es-PY")}</td>
                  <td className="px-3.5 py-2.5">{r.clienteRazonSocial}</td>
                  <td className="px-3.5 py-2.5 text-texto-suave">{r.motivo ?? "—"}</td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
