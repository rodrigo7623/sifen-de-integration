import type { InputHTMLAttributes } from "react";

interface MontoInputProps extends Omit<InputHTMLAttributes<HTMLInputElement>, "value" | "onChange" | "type"> {
  /** null = campo vacío (se muestra el placeholder en vez de un 0). */
  value: number | null;
  onChange: (valor: number | null) => void;
}

const FORMATO = new Intl.NumberFormat("es-PY", { maximumFractionDigits: 0 });

/**
 * Monto en guaraníes (sin decimales) con separador de miles mientras se escribe: "280.000".
 * Reemplaza al input type=number, que mostraba un 0 inicial que quedaba pegado al número
 * tecleado ("0280000") y aceptaba decimales que el guaraní no tiene.
 */
export function MontoInput({ value, onChange, placeholder = "0", ...rest }: MontoInputProps) {
  return (
    <input
      {...rest}
      type="text"
      inputMode="numeric"
      autoComplete="off"
      placeholder={placeholder}
      value={value === null || value === 0 ? "" : FORMATO.format(value)}
      onChange={(e) => {
        const digitos = e.target.value.replace(/\D/g, "").slice(0, 13);
        onChange(digitos === "" ? null : Number(digitos));
      }}
    />
  );
}
