import { apiClient } from "../../api/client";
import type { CondicionPago, EstadoDte, Factura, MotivoNota, TasaIva, TipoDocumento } from "../../api/types";

export interface ItemFacturaInput {
  productoCodigo: string | null;
  descripcion: string;
  cantidad: number;
  precioUnitario: number;
  tasaIva: TasaIva;
}

export interface FacturaInput {
  clienteRuc: string;
  establecimientoId: string;
  puntoExpedicionId: string;
  condicionPago: CondicionPago;
  plazoDias: number | null;
  cantidadCuotas: number | null;
  items: ItemFacturaInput[];
}

export interface NotaInput {
  facturaAsociadaId: string;
  tipo: TipoDocumento;
  motivo: MotivoNota;
  items: ItemFacturaInput[];
}

export const facturasApi = {
  async listar(estado?: EstadoDte | "", tipo?: TipoDocumento | ""): Promise<Factura[]> {
    const { data } = await apiClient.get<Factura[]>("/facturas", {
      params: { ...(estado ? { estado } : {}), ...(tipo ? { tipo } : {}) },
    });
    return data;
  },
  async notas(facturaId: string): Promise<Factura[]> {
    const { data } = await apiClient.get<Factura[]>(`/facturas/${facturaId}/notas`);
    return data;
  },
  async crearNota(input: NotaInput): Promise<Factura> {
    const { data } = await apiClient.post<Factura>("/notas", input);
    return data;
  },
  async editarNota(id: string, input: NotaInput): Promise<Factura> {
    const { data } = await apiClient.put<Factura>(`/notas/${id}`, input);
    return data;
  },
  async obtener(id: string): Promise<Factura> {
    const { data } = await apiClient.get<Factura>(`/facturas/${id}`);
    return data;
  },
  async crear(input: FacturaInput): Promise<Factura> {
    const { data } = await apiClient.post<Factura>("/facturas", input);
    return data;
  },
  async editar(id: string, input: FacturaInput): Promise<Factura> {
    const { data } = await apiClient.put<Factura>(`/facturas/${id}`, input);
    return data;
  },
  async confirmar(id: string): Promise<Factura> {
    const { data } = await apiClient.post<Factura>(`/facturas/${id}/confirmar`, {});
    return data;
  },
  async reenviarCorreo(id: string): Promise<Factura> {
    const { data } = await apiClient.post<Factura>(`/facturas/${id}/reenviar-correo`, {});
    return data;
  },
  async verificar(id: string): Promise<Factura> {
    const { data } = await apiClient.post<Factura>(`/facturas/${id}/verificar`, {});
    return data;
  },
  async reabrir(id: string): Promise<Factura> {
    const { data } = await apiClient.post<Factura>(`/facturas/${id}/reabrir`, {});
    return data;
  },
  async descargarPdf(id: string): Promise<Blob> {
    const { data } = await apiClient.get<Blob>(`/facturas/${id}/pdf`, { responseType: "blob" });
    return data;
  },
  async descargarXml(id: string): Promise<Blob> {
    const { data } = await apiClient.get<Blob>(`/facturas/${id}/xml`, { responseType: "blob" });
    return data;
  },
};

/** Mismo nombre que arma el backend: factura-001-001-0000001, nota-credito-…, nota-debito-…. */
export function nombreArchivo(f: Pick<Factura, "id" | "numero" | "tipoDocumento">, extension: string) {
  const prefijo = f.tipoDocumento === "NC" ? "nota-credito" : f.tipoDocumento === "ND" ? "nota-debito" : "factura";
  return `${prefijo}-${f.numero ?? f.id}.${extension}`;
}

/** Dispara la descarga de un blob en el navegador con el nombre de archivo dado. */
export function descargarArchivo(blob: Blob, nombreArchivo: string) {
  const url = URL.createObjectURL(blob);
  const enlace = document.createElement("a");
  enlace.href = url;
  enlace.download = nombreArchivo;
  document.body.appendChild(enlace);
  enlace.click();
  document.body.removeChild(enlace);
  URL.revokeObjectURL(url);
}
