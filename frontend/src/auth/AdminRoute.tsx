import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "./AuthContext";

/**
 * Se usa anidada dentro de <ProtectedRoute> (ya garantiza que hay sesión). Si el usuario
 * autenticado no es ADMIN, lo manda de vuelta al panel en vez de mostrar la pantalla — el backend
 * igual rechaza estas rutas con 403, esto es solo para no ofrecer una pantalla que va a fallar.
 */
export function AdminRoute() {
  const { usuario } = useAuth();

  if (usuario?.rol !== "ADMIN") {
    return <Navigate to="/facturas" replace />;
  }

  return <Outlet />;
}
