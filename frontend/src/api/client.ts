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

/** Evento que dispara el cliente cuando el backend rechaza el token (BUG-010); lo escucha AuthProvider. */
export const SESION_VENCIDA_EVENTO = "sifen:sesion-vencida";
export const SESION_VENCIDA_AVISO_KEY = "sifen.auth.aviso";

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    // Un 401 fuera del login significa que el token venció, es inválido o el usuario se desactivó:
    // se cierra la sesión y se vuelve al login, en vez de dejar todas las pantallas mostrando errores.
    // En el login, el 401 es "contraseña incorrecta" y lo muestra el formulario.
    const url: string = error?.config?.url ?? "";
    if (axios.isAxiosError(error) && error.response?.status === 401 && !url.includes("/auth/")) {
      const data = error.response.data as { message?: string } | undefined;
      sessionStorage.setItem(SESION_VENCIDA_AVISO_KEY, data?.message ?? "Tu sesión venció. Volvé a iniciar sesión.");
      window.dispatchEvent(new Event(SESION_VENCIDA_EVENTO));
    }
    return Promise.reject(error);
  },
);

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
