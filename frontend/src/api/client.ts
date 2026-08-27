import axios from "axios";

export const TOKEN_STORAGE_KEY = "sifen.auth.token";

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080/api",
});

apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_STORAGE_KEY);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

/** Mensaje de error legible a partir de la respuesta de la API (ver ApiError del backend). */
export function extraerMensajeError(error: unknown, mensajePorDefecto: string): string {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data as { message?: string; detalles?: string[] } | undefined;
    if (data?.detalles?.length) {
      return data.detalles.join(" · ");
    }
    if (data?.message) {
      return data.message;
    }
  }
  return mensajePorDefecto;
}
