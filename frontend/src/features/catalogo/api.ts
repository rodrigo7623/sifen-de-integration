import { apiClient } from "../../api/client";
import type { Producto, TasaIva } from "../../api/types";

export interface ProductoInput {
  codigo: string;
  descripcion: string;
  unidadMedida: string;
  precioBase: number;
  tasaIva: TasaIva;
}

export const productosApi = {
  async buscar(q?: string, incluirInactivos?: boolean): Promise<Producto[]> {
    const { data } = await apiClient.get<Producto[]>("/productos", { params: { q, incluirInactivos } });
    return data;
  },
  async crear(input: ProductoInput): Promise<Producto> {
    const { data } = await apiClient.post<Producto>("/productos", input);
    return data;
  },
  async editar(codigo: string, input: ProductoInput): Promise<Producto> {
    const { data } = await apiClient.put<Producto>(`/productos/${encodeURIComponent(codigo)}`, input);
    return data;
  },
  async desactivar(codigo: string): Promise<void> {
    await apiClient.delete(`/productos/${encodeURIComponent(codigo)}`);
  },
};
