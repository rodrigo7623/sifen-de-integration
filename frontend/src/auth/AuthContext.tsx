import { createContext, useContext, useMemo, useState, type ReactNode } from "react";
import { apiClient, TOKEN_STORAGE_KEY } from "../api/client";

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

  const value = useMemo<AuthContextValue>(
    () => ({
      usuario,
      async login(email: string, password: string) {
        const { data } = await apiClient.post("/auth/login", { email, password });
        localStorage.setItem(TOKEN_STORAGE_KEY, data.token);
        const nuevoUsuario: Usuario = { nombre: data.nombre, email: data.email, rol: data.rol };
        localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(nuevoUsuario));
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
