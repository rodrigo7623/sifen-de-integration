import { Navigate, Route, Routes } from "react-router-dom";
import { AuthProvider } from "./auth/AuthContext";
import { LoginPage } from "./auth/LoginPage";
import { ProtectedRoute } from "./auth/ProtectedRoute";
import { AdminRoute } from "./auth/AdminRoute";
import { AuditoriaPage } from "./features/auditoria/AuditoriaPage";
import { ClientesPage } from "./features/clientes/ClientesPage";
import { ComprobantesPage } from "./features/compras/ComprobantesPage";
import { ConfiguracionPage } from "./features/configuracion/ConfiguracionPage";
import { DashboardPage } from "./features/dashboard/DashboardPage";
import { EstablecimientosPage } from "./features/establecimientos/EstablecimientosPage";
import { ProductosPage } from "./features/catalogo/ProductosPage";
import { FacturasPage } from "./features/facturas/FacturasPage";
import { FacturaFormPage } from "./features/facturas/FacturaFormPage";
import { NotaFormPage } from "./features/facturas/NotaFormPage";
import { LibroIvaPage } from "./features/reportes/LibroIvaPage";
import { RechazosPage } from "./features/reportes/RechazosPage";
import { UsuariosPage } from "./features/usuarios/UsuariosPage";
import { AppLayout } from "./layout/AppLayout";

export function App() {
  return (
    <AuthProvider>
      <Routes>
        <Route path="/login" element={<LoginPage />} />

        <Route element={<ProtectedRoute />}>
          <Route element={<AppLayout />}>
            <Route path="/" element={<DashboardPage />} />
            <Route path="/facturas" element={<FacturasPage />} />
            <Route path="/facturas/nueva" element={<FacturaFormPage />} />
            <Route path="/facturas/:id" element={<FacturaFormPage />} />
            <Route path="/notas/nueva" element={<NotaFormPage />} />
            <Route path="/notas/:id" element={<NotaFormPage />} />
            <Route path="/productos" element={<ProductosPage />} />
            <Route path="/clientes" element={<ClientesPage />} />
            <Route path="/comprobantes" element={<ComprobantesPage />} />
            <Route path="/reportes/libro-iva" element={<LibroIvaPage />} />
            <Route path="/reportes/rechazos" element={<RechazosPage />} />
            <Route element={<AdminRoute />}>
              <Route path="/usuarios" element={<UsuariosPage />} />
              <Route path="/auditoria" element={<AuditoriaPage />} />
              <Route path="/establecimientos" element={<EstablecimientosPage />} />
              <Route path="/configuracion" element={<ConfiguracionPage />} />
            </Route>
          </Route>
        </Route>

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </AuthProvider>
  );
}
