import { Navigate, Route, Routes } from "react-router-dom";
import { AuthProvider } from "./auth/AuthContext";
import { LoginPage } from "./auth/LoginPage";
import { ProtectedRoute } from "./auth/ProtectedRoute";
import { AdminRoute } from "./auth/AdminRoute";
import { ClientesPage } from "./features/clientes/ClientesPage";
import { ProductosPage } from "./features/catalogo/ProductosPage";
import { FacturasPage } from "./features/facturas/FacturasPage";
import { FacturaFormPage } from "./features/facturas/FacturaFormPage";
import { UsuariosPage } from "./features/usuarios/UsuariosPage";
import { AppLayout } from "./layout/AppLayout";

export function App() {
  return (
    <AuthProvider>
      <Routes>
        <Route path="/login" element={<LoginPage />} />

        <Route element={<ProtectedRoute />}>
          <Route element={<AppLayout />}>
            <Route path="/facturas" element={<FacturasPage />} />
            <Route path="/facturas/nueva" element={<FacturaFormPage />} />
            <Route path="/facturas/:id" element={<FacturaFormPage />} />
            <Route path="/productos" element={<ProductosPage />} />
            <Route path="/clientes" element={<ClientesPage />} />
            <Route element={<AdminRoute />}>
              <Route path="/usuarios" element={<UsuariosPage />} />
            </Route>
            <Route path="/" element={<Navigate to="/facturas" replace />} />
          </Route>
        </Route>

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </AuthProvider>
  );
}
