import { apiClient } from "../../api/client";
import type { CondicionPago, EstadoDte, Factura, TasaIva } from "../../api/types";

export interface ItemFacturaInput {
  productoCodigo: string | null;
  descripcion: string;
  cantidad: number;
  precioUnitario: number;
  tasaIva: TasaIva;
}

export interface FacturaInput {
  clienteRuc: string;
  condicionPago: CondicionPago;
  plazoDias: number | null;
  cantidadCuotas: number | null;
  items: ItemFacturaInput[];
}

export const facturasApi = {
  async listar(estado?: EstadoDte | ""): Promise<Factura[]> {
    const { data } = await apiClient.get<Factura[]>("/facturas", {
      params: estado ? { estado } : undefined,
    });
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
};
