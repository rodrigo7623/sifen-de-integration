import { createContext, useCallback, useContext, useRef, useState, type ReactNode } from "react";
import { Button } from "./Button";
import { Modal } from "./Modal";

interface ConfirmState {
  message: string;
  resolve: (value: boolean) => void;
}

type ConfirmFn = (message: string) => Promise<boolean>;

const ConfirmContext = createContext<ConfirmFn | undefined>(undefined);

/** Reemplaza window.confirm por un modal con estilo, manteniendo la misma firma async simple:
 * `if (!(await confirmar("¿Seguro?"))) return;` */
export function ConfirmProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<ConfirmState | null>(null);
  const resolverRef = useRef<((value: boolean) => void) | null>(null);

  const confirmar = useCallback<ConfirmFn>((message) => {
    return new Promise<boolean>((resolve) => {
      resolverRef.current = resolve;
      setState({ message, resolve });
    });
  }, []);

  function responder(valor: boolean) {
    resolverRef.current?.(valor);
    setState(null);
  }

  return (
    <ConfirmContext.Provider value={confirmar}>
      {children}
      {state && (
        <Modal title="Confirmar acción" onClose={() => responder(false)}>
          <p className="text-sm text-texto">{state.message}</p>
          <div className="mt-5 flex justify-end gap-2">
            <Button variant="secondary" onClick={() => responder(false)}>
              Cancelar
            </Button>
            <Button variant="danger" onClick={() => responder(true)}>
              Confirmar
            </Button>
          </div>
        </Modal>
      )}
    </ConfirmContext.Provider>
  );
}

export function useConfirm(): ConfirmFn {
  const context = useContext(ConfirmContext);
  if (!context) {
    throw new Error("useConfirm debe usarse dentro de un ConfirmProvider");
  }
  return context;
}
