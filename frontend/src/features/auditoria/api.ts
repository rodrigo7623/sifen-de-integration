import { apiClient } from "../../api/client";
import type { AuditoriaEntrada, PaginaSpring, TipoDocumento } from "../../api/types";

export interface FiltrosAuditoria {
  desde?: string;
  hasta?: string;
  operacion?: string;
  usuarioId?: string;
  tipoDocumento?: TipoDocumento | "";
  /** Número del comprobante (001-001-0000005 o 5), RUC o razón social del cliente. */
  q?: string;
}

/** Los filtros vacíos no se mandan. */
function sinVacios(filtros: FiltrosAuditoria): Record<string, string> {
  return Object.fromEntries(Object.entries(filtros).filter(([, v]) => v !== undefined && String(v).trim() !== ""));
}

export const auditoriaApi = {
  async listar(page: number, filtros: FiltrosAuditoria = {}, size = 20): Promise<PaginaSpring<AuditoriaEntrada>> {
    const { data } = await apiClient.get<PaginaSpring<AuditoriaEntrada>>("/auditoria", {
      params: { page, size, ...sinVacios(filtros) },
    });
    return data;
  },
  async operaciones(): Promise<string[]> {
    const { data } = await apiClient.get<string[]>("/auditoria/operaciones");
    return data;
  },
};
