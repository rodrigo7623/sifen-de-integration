import {
  Building2,
  FileText,
  History,
  LayoutDashboard,
  LogOut,
  Menu,
  Package,
  Settings,
  UserCog,
  Users,
  X,
} from "lucide-react";
import { useState, type ElementType, type ReactNode } from "react";
import { NavLink, Outlet } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

const NAV_LINK_BASE =
  "flex items-center gap-2.5 rounded-md px-3 py-2.5 text-sm text-white/85 hover:bg-white/10";
const NAV_LINK_ACTIVE = "bg-azul opacity-100 text-white";

function NavGroup({ title, children }: { title: string; children: ReactNode }) {
  return (
    <div className="mt-5">
      <div className="mb-1 px-3 text-[11px] font-semibold uppercase tracking-wide text-white/50">
        {title}
      </div>
      <div className="flex flex-col gap-1">{children}</div>
    </div>
  );
}

export function AppLayout() {
  const { usuario, logout } = useAuth();
  const esAdmin = usuario?.rol === "ADMIN";
  const [sidebarAbierto, setSidebarAbierto] = useState(false);

  function link(to: string, label: string, Icon: ElementType) {
    return (
      <NavLink
        to={to}
        onClick={() => setSidebarAbierto(false)}
        className={({ isActive }) => `${NAV_LINK_BASE} ${isActive ? NAV_LINK_ACTIVE : ""}`}
      >
        <Icon size={17} />
        {label}
      </NavLink>
    );
  }

  return (
    <div className="flex min-h-screen">
      {sidebarAbierto && (
        <div
          className="fixed inset-0 z-30 bg-slate-900/50 md:hidden"
          onClick={() => setSidebarAbierto(false)}
        />
      )}

      <aside
        className={`fixed inset-y-0 left-0 z-40 flex w-60 flex-col bg-azul-oscuro p-4 text-white transition-transform md:static md:translate-x-0 ${
          sidebarAbierto ? "translate-x-0" : "-translate-x-full"
        }`}
      >
        <div className="flex items-center justify-between">
          <div>
            <div className="text-[1.1rem] font-bold">SIFEN Manager</div>
            <div className="text-xs text-white/70">Tottal Store</div>
          </div>
          <button className="md:hidden" onClick={() => setSidebarAbierto(false)} aria-label="Cerrar menú">
            <X size={20} />
          </button>
        </div>

        <nav className="mt-2">
          <NavGroup title="Operación">
            {link("/", "Inicio", LayoutDashboard)}
            {link("/facturas", "Facturas", FileText)}
            {link("/productos", "Productos", Package)}
            {link("/clientes", "Clientes", Users)}
          </NavGroup>

          {esAdmin && (
            <NavGroup title="Administración">
              {link("/usuarios", "Usuarios", UserCog)}
              {link("/auditoria", "Auditoría", History)}
              {link("/establecimientos", "Establecimientos", Building2)}
              {link("/configuracion", "Configuración", Settings)}
            </NavGroup>
          )}
        </nav>
      </aside>

      <div className="flex min-h-screen flex-1 flex-col">
        <header className="flex items-center justify-between gap-4 border-b border-borde bg-white px-4 py-3.5 md:justify-end md:px-7">
          <button
            className="rounded-md p-1.5 hover:bg-azul-claro md:hidden"
            onClick={() => setSidebarAbierto(true)}
            aria-label="Abrir menú"
          >
            <Menu size={22} />
          </button>
          <div className="flex items-center gap-4">
            <span className="text-sm">
              {usuario?.nombre} <span className="text-texto-suave">({usuario?.rol})</span>
            </span>
            <button
              className="flex items-center gap-1 rounded-md px-1.5 py-0.5 text-sm text-azul hover:bg-azul-claro"
              onClick={logout}
            >
              <LogOut size={16} />
              Cerrar sesión
            </button>
          </div>
        </header>
        <main className="flex-1 p-4 md:p-7">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
