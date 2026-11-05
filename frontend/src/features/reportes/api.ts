import { apiClient } from "../../api/client";
import type { HistorialCliente, LibroIva, LiquidacionIva, ReporteRechazos, TipoLibro } from "../../api/types";

export const reportesApi = {
  async libroIva(periodo: string, libro: TipoLibro = "VENTAS"): Promise<LibroIva> {
    const { data } = await apiClient.get<LibroIva>("/reportes/libro-iva", { params: { periodo, libro } });
    return data;
  },
  async libroIvaArchivo(periodo: string, formato: "excel" | "pdf", libro: TipoLibro = "VENTAS"): Promise<Blob> {
    const { data } = await apiClient.get<Blob>(`/reportes/libro-iva/${formato}`, {
      params: { periodo, libro },
      responseType: "blob",
    });
    return data;
  },
  async liquidacionIva(periodo: string): Promise<LiquidacionIva> {
    const { data } = await apiClient.get<LiquidacionIva>("/reportes/liquidacion-iva", { params: { periodo } });
    return data;
  },
  /** ZIP para importar en Marangatu; el nombre (con el identificador único) lo arma el backend. */
  async registroRg90(periodo: string): Promise<{ blob: Blob; nombre: string }> {
    const respuesta = await apiClient.get<Blob>("/reportes/registro-rg90", {
      params: { periodo },
      responseType: "blob",
    });
    const disposicion = String(respuesta.headers["content-disposition"] ?? "");
    const nombre = /filename="([^"]+)"/.exec(disposicion)?.[1] ?? `registro-comprobantes-${periodo}.zip`;
    return { blob: respuesta.data, nombre };
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

/** Un error de una descarga con responseType "blob" trae el JSON de ApiError dentro de un Blob. */
export async function mensajeDeBlob(error: unknown, porDefecto: string): Promise<string> {
  const data = (error as { response?: { data?: unknown } })?.response?.data;
  if (data instanceof Blob) {
    try {
      const json = JSON.parse(await data.text()) as { message?: string };
      if (json.message) return json.message;
    } catch {
      /* no era JSON */
    }
  }
  return porDefecto;
}
