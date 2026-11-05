import { X } from "lucide-react";
import { useEffect, type ReactNode } from "react";

interface ModalProps {
  title: string;
  onClose: () => void;
  children: ReactNode;
  footer?: ReactNode;
  maxWidthClassName?: string;
}

/**
 * Ventana modal de los ABM. No se cierra al hacer clic fuera del formulario (se perderían los datos
 * cargados a medias): se cierra solo con la X, el botón Cancelar del pie o la tecla Escape.
 */
export function Modal({ title, onClose, children, footer, maxWidthClassName = "max-w-md" }: ModalProps) {
  useEffect(() => {
    const alPresionar = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    window.addEventListener("keydown", alPresionar);
    return () => window.removeEventListener("keydown", alPresionar);
  }, [onClose]);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/45 p-5">
      <div
        role="dialog"
        aria-modal="true"
        aria-label={title}
        className={`relative w-full ${maxWidthClassName} max-h-[90vh] overflow-y-auto rounded-xl bg-white p-6 shadow-modal`}
      >
        <button
          type="button"
          onClick={onClose}
          aria-label="Cerrar"
          title="Cerrar"
          className="absolute right-3 top-3 rounded-md p-1.5 text-texto-suave hover:bg-slate-100 hover:text-texto"
        >
          <X size={20} />
        </button>
        <h2 className="mt-0 mb-4 pr-8 text-lg font-semibold text-azul-oscuro">{title}</h2>
        {children}
        {footer && <div className="mt-5 flex justify-end gap-2">{footer}</div>}
      </div>
    </div>
  );
}
