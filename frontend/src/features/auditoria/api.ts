import { apiClient } from "../../api/client";
import type { AuditoriaEntrada, PaginaSpring } from "../../api/types";

export const auditoriaApi = {
  async listar(page: number, size = 20): Promise<PaginaSpring<AuditoriaEntrada>> {
    const { data } = await apiClient.get<PaginaSpring<AuditoriaEntrada>>("/auditoria", {
      params: { page, size },
    });
    return data;
  },
};
