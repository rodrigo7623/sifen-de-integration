import { apiClient } from "../../api/client";
import type { CatalogoRg90, ComprobanteRegistrado, ComprobanteRegistradoInput, TipoRegistro } from "../../api/types";

export const comprobantesApi = {
  async listar(periodo: string, tipo?: TipoRegistro): Promise<ComprobanteRegistrado[]> {
    const { data } = await apiClient.get<ComprobanteRegistrado[]>("/comprobantes-registrados", {
      params: { periodo, tipo: tipo || undefined },
    });
    return data;
  },
  async catalogo(): Promise<CatalogoRg90> {
    const { data } = await apiClient.get<CatalogoRg90>("/comprobantes-registrados/catalogo");
    return data;
  },
  async crear(input: ComprobanteRegistradoInput): Promise<ComprobanteRegistrado> {
    const { data } = await apiClient.post<ComprobanteRegistrado>("/comprobantes-registrados", input);
    return data;
  },
  async editar(id: string, input: ComprobanteRegistradoInput): Promise<ComprobanteRegistrado> {
    const { data } = await apiClient.put<ComprobanteRegistrado>(`/comprobantes-registrados/${id}`, input);
    return data;
  },
  async eliminar(id: string): Promise<void> {
    await apiClient.delete(`/comprobantes-registrados/${id}`);
  },
};
