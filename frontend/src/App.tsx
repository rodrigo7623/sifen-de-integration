import { Navigate, Route, Routes } from "react-router-dom";
import { AuthProvider } from "./auth/AuthContext";
import { LoginPage } from "./auth/LoginPage";
import { ProtectedRoute } from "./auth/ProtectedRoute";
import { ClientesPage } from "./features/clientes/ClientesPage";
import { ProductosPage } from "./features/catalogo/ProductosPage";
import { AppLayout } from "./layout/AppLayout";

export function App() {
  return (
    <AuthProvider>
      <Routes>
        <Route path="/login" element={<LoginPage />} />

        <Route element={<ProtectedRoute />}>
          <Route element={<AppLayout />}>
            <Route path="/productos" element={<ProductosPage />} />
            <Route path="/clientes" element={<ClientesPage />} />
            <Route path="/" element={<Navigate to="/productos" replace />} />
          </Route>
        </Route>

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </AuthProvider>
  );
}
