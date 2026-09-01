import { NavLink, Outlet } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

export function AppLayout() {
  const { usuario, logout } = useAuth();

  return (
    <div className="app-layout">
      <aside className="sidebar">
        <div className="sidebar-brand">SIFEN Manager</div>
        <div className="sidebar-subbrand">Tottal Store</div>
        <nav>
          <NavLink to="/facturas" className={({ isActive }) => (isActive ? "active" : "")}>
            Facturas
          </NavLink>
          <NavLink to="/productos" className={({ isActive }) => (isActive ? "active" : "")}>
            Productos
          </NavLink>
          <NavLink to="/clientes" className={({ isActive }) => (isActive ? "active" : "")}>
            Clientes
          </NavLink>
          {usuario?.rol === "ADMIN" && (
            <NavLink to="/usuarios" className={({ isActive }) => (isActive ? "active" : "")}>
              Usuarios
            </NavLink>
          )}
        </nav>
      </aside>
      <div className="main-area">
        <header className="topbar">
          <span>
            {usuario?.nombre} <span className="hint-text">({usuario?.rol})</span>
          </span>
          <button className="link-button" onClick={logout}>
            Cerrar sesión
          </button>
        </header>
        <main className="content">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
