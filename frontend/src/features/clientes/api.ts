import { apiClient } from "../../api/client";
import type { Cliente, CondicionIva, ResultadoValidacionRuc } from "../../api/types";

export interface ClienteInput {
  ruc: string;
  razonSocial: string;
  direccion: string;
  email: string;
  condicionIva: CondicionIva;
}

export const clientesApi = {
  async buscar(q?: string, incluirInactivos?: boolean): Promise<Cliente[]> {
    const { data } = await apiClient.get<Cliente[]>("/clientes", { params: { q, incluirInactivos } });
    return data;
  },
  async crear(input: ClienteInput): Promise<Cliente> {
    const { data } = await apiClient.post<Cliente>("/clientes", input);
    return data;
  },
  async editar(ruc: string, input: ClienteInput): Promise<Cliente> {
    const { data } = await apiClient.put<Cliente>(`/clientes/${encodeURIComponent(ruc)}`, input);
    return data;
  },
  async desactivar(ruc: string): Promise<void> {
    await apiClient.delete(`/clientes/${encodeURIComponent(ruc)}`);
  },
  async validarRuc(ruc: string): Promise<ResultadoValidacionRuc> {
    const { data } = await apiClient.get<ResultadoValidacionRuc>("/clientes/validar-ruc", {
      params: { ruc },
    });
    return data;
  },
};
