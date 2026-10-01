import { apiClient } from "../../api/client";
import type { ConfiguracionEmisor } from "../../api/types";

export const configuracionApi = {
  async obtener(): Promise<ConfiguracionEmisor> {
    const { data } = await apiClient.get<ConfiguracionEmisor>("/configuracion/emisor");
    return data;
  },
  async actualizar(input: ConfiguracionEmisor): Promise<ConfiguracionEmisor> {
    const { data } = await apiClient.put<ConfiguracionEmisor>("/configuracion/emisor", input);
    return data;
  },
};
