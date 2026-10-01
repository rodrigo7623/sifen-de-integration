import { useState, type FormEvent } from "react";
import { useNavigate } from "react-router-dom";
import { extraerMensajeError } from "../api/client";
import { Button } from "../components/ui/Button";
import { useAuth } from "./AuthContext";

export function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState("admin@tottalstore.com");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [cargando, setCargando] = useState(false);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setCargando(true);
    try {
      await login(email, password);
      navigate("/");
    } catch (err) {
      setError(extraerMensajeError(err, "No se pudo iniciar sesión"));
    } finally {
      setCargando(false);
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-azul-oscuro to-azul p-5">
      <form className="w-full max-w-[360px] rounded-xl bg-white p-9 shadow-login" onSubmit={onSubmit}>
        <h1 className="m-0 mb-1 text-xl font-bold text-azul-oscuro">SIFEN Manager</h1>
        <p className="m-0 mb-5 text-sm text-texto-suave">
          Sistema de Facturación Electrónica · Tottal Store
        </p>

        <label htmlFor="email" className="mb-1 block text-sm font-semibold text-texto-suave">
          Usuario
        </label>
        <input
          id="email"
          type="email"
          className="w-full rounded-md border border-borde px-2.5 py-2 text-sm"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
        />

        <label htmlFor="password" className="mb-1 mt-3 block text-sm font-semibold text-texto-suave">
          Contraseña
        </label>
        <input
          id="password"
          type="password"
          className="w-full rounded-md border border-borde px-2.5 py-2 text-sm"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
        />

        {error && <p className="mt-2 text-sm text-rojo">{error}</p>}

        <Button type="submit" disabled={cargando} className="mt-5 w-full py-2.5">
          {cargando ? "Ingresando…" : "Iniciar sesión"}
        </Button>
      </form>
    </div>
  );
}
