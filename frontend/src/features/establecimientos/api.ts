import { apiClient } from "../../api/client";
import type { Establecimiento } from "../../api/types";

export interface EstablecimientoInput {
  codigo: string;
  denominacion: string;
  direccion: string;
  numeroCasa: string;
  departamentoCodigo: string;
  departamentoDescripcion: string;
  distritoCodigo: string;
  distritoDescripcion: string;
  ciudadCodigo: string;
  ciudadDescripcion: string;
  telefono: string;
  email: string;
}

export interface PuntoExpedicionInput {
  codigo: string;
  descripcion: string;
}

export const establecimientosApi = {
  async listar(incluirInactivos = false): Promise<Establecimiento[]> {
    const { data } = await apiClient.get<Establecimiento[]>("/establecimientos", {
      params: { incluirInactivos },
    });
    return data;
  },
  async crear(input: EstablecimientoInput): Promise<Establecimiento> {
    const { data } = await apiClient.post<Establecimiento>("/establecimientos", input);
    return data;
  },
  async editar(id: string, input: EstablecimientoInput): Promise<Establecimiento> {
    const { data } = await apiClient.put<Establecimiento>(`/establecimientos/${id}`, input);
    return data;
  },
  async activar(id: string): Promise<Establecimiento> {
    const { data } = await apiClient.post<Establecimiento>(`/establecimientos/${id}/activar`);
    return data;
  },
  async desactivar(id: string): Promise<Establecimiento> {
    const { data } = await apiClient.post<Establecimiento>(`/establecimientos/${id}/desactivar`);
    return data;
  },
  async agregarPunto(establecimientoId: string, input: PuntoExpedicionInput): Promise<Establecimiento> {
    const { data } = await apiClient.post<Establecimiento>(
      `/establecimientos/${establecimientoId}/puntos-expedicion`,
      input,
    );
    return data;
  },
  async editarPunto(
    establecimientoId: string,
    puntoId: string,
    input: PuntoExpedicionInput,
  ): Promise<Establecimiento> {
    const { data } = await apiClient.put<Establecimiento>(
      `/establecimientos/${establecimientoId}/puntos-expedicion/${puntoId}`,
      input,
    );
    return data;
  },
  async activarPunto(establecimientoId: string, puntoId: string): Promise<Establecimiento> {
    const { data } = await apiClient.post<Establecimiento>(
      `/establecimientos/${establecimientoId}/puntos-expedicion/${puntoId}/activar`,
    );
    return data;
  },
  async desactivarPunto(establecimientoId: string, puntoId: string): Promise<Establecimiento> {
    const { data } = await apiClient.post<Establecimiento>(
      `/establecimientos/${establecimientoId}/puntos-expedicion/${puntoId}/desactivar`,
    );
    return data;
  },
};
