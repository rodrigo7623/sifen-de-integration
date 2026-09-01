import { useState, type FormEvent } from "react";
import { useNavigate } from "react-router-dom";
import { extraerMensajeError } from "../api/client";
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
      navigate("/facturas");
    } catch (err) {
      setError(extraerMensajeError(err, "No se pudo iniciar sesión"));
    } finally {
      setCargando(false);
    }
  }

  return (
    <div className="login-page">
      <form className="login-card" onSubmit={onSubmit}>
        <h1>SIFEN Manager</h1>
        <p className="subtitle">Sistema de Facturación Electrónica · Tottal Store</p>

        <label htmlFor="email">Usuario</label>
        <input
          id="email"
          type="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
        />

        <label htmlFor="password">Contraseña</label>
        <input
          id="password"
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
        />

        {error && <p className="error-text">{error}</p>}

        <button type="submit" disabled={cargando}>
          {cargando ? "Ingresando…" : "Iniciar sesión"}
        </button>
      </form>
    </div>
  );
}
