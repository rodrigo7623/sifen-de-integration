import { apiClient } from "../../api/client";
import type { Rol, Usuario } from "../../api/types";

export interface UsuarioCrearInput {
  nombre: string;
  email: string;
  password: string;
  rol: Rol;
}

export interface UsuarioEditarInput {
  nombre: string;
  rol: Rol;
  password: string | null;
}

export const usuariosApi = {
  async buscar(q?: string): Promise<Usuario[]> {
    const { data } = await apiClient.get<Usuario[]>("/usuarios", { params: { q } });
    return data;
  },
  async crear(input: UsuarioCrearInput): Promise<Usuario> {
    const { data } = await apiClient.post<Usuario>("/usuarios", input);
    return data;
  },
  async editar(id: string, input: UsuarioEditarInput): Promise<Usuario> {
    const { data } = await apiClient.put<Usuario>(`/usuarios/${id}`, input);
    return data;
  },
  async activar(id: string): Promise<void> {
    await apiClient.post(`/usuarios/${id}/activar`);
  },
  async desactivar(id: string): Promise<void> {
    await apiClient.post(`/usuarios/${id}/desactivar`);
  },
};
