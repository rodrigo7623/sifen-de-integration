import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import { apiClient, SESION_VENCIDA_EVENTO, SESION_VENCIDA_AVISO_KEY, TOKEN_STORAGE_KEY } from "../api/client";

interface Usuario {
  nombre: string;
  email: string;
  rol: string;
}

interface AuthContextValue {
  usuario: Usuario | null;
  login: (email: string, password: string) => Promise<void>;
  logout: () => void;
}

const USER_STORAGE_KEY = "sifen.auth.user";

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

function leerUsuarioGuardado(): Usuario | null {
  const raw = localStorage.getItem(USER_STORAGE_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as Usuario;
  } catch {
    return null;
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [usuario, setUsuario] = useState<Usuario | null>(leerUsuarioGuardado);

  // BUG-010: el interceptor de apiClient avisa cuando el backend responde 401 (token vencido).
  useEffect(() => {
    function alVencer() {
      localStorage.removeItem(TOKEN_STORAGE_KEY);
      localStorage.removeItem(USER_STORAGE_KEY);
      setUsuario(null);
    }
    window.addEventListener(SESION_VENCIDA_EVENTO, alVencer);
    return () => window.removeEventListener(SESION_VENCIDA_EVENTO, alVencer);
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      usuario,
      async login(email: string, password: string) {
        const { data } = await apiClient.post("/auth/login", { email, password });
        localStorage.setItem(TOKEN_STORAGE_KEY, data.token);
        const nuevoUsuario: Usuario = { nombre: data.nombre, email: data.email, rol: data.rol };
        localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(nuevoUsuario));
        sessionStorage.removeItem(SESION_VENCIDA_AVISO_KEY);
        setUsuario(nuevoUsuario);
      },
      logout() {
        localStorage.removeItem(TOKEN_STORAGE_KEY);
        localStorage.removeItem(USER_STORAGE_KEY);
        setUsuario(null);
      },
    }),
    [usuario],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth debe usarse dentro de un AuthProvider");
  }
  return context;
}
