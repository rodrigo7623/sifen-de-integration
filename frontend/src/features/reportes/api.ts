import { apiClient } from "../../api/client";
import type { HistorialCliente, LibroIva, ReporteRechazos } from "../../api/types";

export const reportesApi = {
  async libroIva(periodo: string): Promise<LibroIva> {
    const { data } = await apiClient.get<LibroIva>("/reportes/libro-iva", { params: { periodo } });
    return data;
  },
  async libroIvaArchivo(periodo: string, formato: "excel" | "pdf"): Promise<Blob> {
    const { data } = await apiClient.get<Blob>(`/reportes/libro-iva/${formato}`, {
      params: { periodo },
      responseType: "blob",
    });
    return data;
  },
  async rechazos(desde?: string, hasta?: string): Promise<ReporteRechazos> {
    const { data } = await apiClient.get<ReporteRechazos>("/reportes/rechazos", {
      params: { desde: desde || undefined, hasta: hasta || undefined },
    });
    return data;
  },
  async historialCliente(ruc: string): Promise<HistorialCliente> {
    const { data } = await apiClient.get<HistorialCliente>(`/reportes/clientes/${encodeURIComponent(ruc)}`);
    return data;
  },
};

export function guaranies(valor: number): string {
  return valor.toLocaleString("es-PY");
}
