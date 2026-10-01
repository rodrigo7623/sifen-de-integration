import type { ReactNode } from "react";

interface ModalProps {
  title: string;
  onClose: () => void;
  children: ReactNode;
  footer?: ReactNode;
  maxWidthClassName?: string;
}

export function Modal({ title, onClose, children, footer, maxWidthClassName = "max-w-md" }: ModalProps) {
  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/45 p-5"
      onClick={onClose}
    >
      <div
        className={`w-full ${maxWidthClassName} max-h-[90vh] overflow-y-auto rounded-xl bg-white p-6 shadow-modal`}
        onClick={(e) => e.stopPropagation()}
      >
        <h2 className="mt-0 mb-4 text-lg font-semibold text-azul-oscuro">{title}</h2>
        {children}
        {footer && <div className="mt-5 flex justify-end gap-2">{footer}</div>}
      </div>
    </div>
  );
}
