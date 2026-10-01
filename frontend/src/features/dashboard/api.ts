import { apiClient } from "../../api/client";
import type { ResumenDashboard } from "../../api/types";

export const dashboardApi = {
  async resumen(): Promise<ResumenDashboard> {
    const { data } = await apiClient.get<ResumenDashboard>("/dashboard/resumen");
    return data;
  },
};
