import { CheckCircle2, Info, X, XCircle } from "lucide-react";
import { createContext, useCallback, useContext, useState, type ReactNode } from "react";

type ToastType = "success" | "error" | "info";

interface Toast {
  id: number;
  type: ToastType;
  message: string;
}

interface ToastContextValue {
  showToast: (message: string, type?: ToastType) => void;
}

const ToastContext = createContext<ToastContextValue | undefined>(undefined);

const ICONS: Record<ToastType, ReactNode> = {
  success: <CheckCircle2 size={24} className="shrink-0 text-verde" />,
  error: <XCircle size={24} className="shrink-0 text-rojo" />,
  info: <Info size={24} className="shrink-0 text-azul" />,
};

const BORDE: Record<ToastType, string> = {
  success: "border-l-verde",
  error: "border-l-rojo",
  info: "border-l-azul",
};

/** Los errores quedan más tiempo en pantalla: suelen traer un mensaje que hay que leer completo. */
const DURATION_MS: Record<ToastType, number> = {
  success: 6000,
  info: 8000,
  error: 12000,
};

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([]);

  const cerrar = useCallback((id: number) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  const showToast = useCallback(
    (message: string, type: ToastType = "info") => {
      const id = Date.now() + Math.random();
      setToasts((prev) => [...prev, { id, type, message }]);
      setTimeout(() => cerrar(id), DURATION_MS[type]);
    },
    [cerrar],
  );

  return (
    <ToastContext.Provider value={{ showToast }}>
      {children}
      <div className="fixed bottom-6 right-6 z-[100] flex w-[min(28rem,calc(100vw-3rem))] flex-col gap-3">
        {toasts.map((t) => (
          <div
            key={t.id}
            role={t.type === "error" ? "alert" : "status"}
            className={`flex items-start gap-3 rounded-lg border-l-4 ${BORDE[t.type]} bg-white px-5 py-4 text-base shadow-modal`}
          >
            {ICONS[t.type]}
            <span className="flex-1 leading-snug">{t.message}</span>
            <button
              type="button"
              onClick={() => cerrar(t.id)}
              aria-label="Cerrar aviso"
              className="rounded p-0.5 text-texto-suave hover:bg-slate-100 hover:text-texto"
            >
              <X size={18} />
            </button>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
}

export function useToast(): ToastContextValue {
  const context = useContext(ToastContext);
  if (!context) {
    throw new Error("useToast debe usarse dentro de un ToastProvider");
  }
  return context;
}
