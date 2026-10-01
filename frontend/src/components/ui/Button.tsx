import type { ButtonHTMLAttributes, ReactNode } from "react";

type Variant = "primary" | "secondary" | "danger" | "link" | "link-danger";

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant;
  icon?: ReactNode;
  children?: ReactNode;
}

const BASE =
  "inline-flex items-center justify-center gap-1.5 rounded-md text-sm font-medium transition-colors disabled:opacity-60 disabled:cursor-not-allowed";

const VARIANT_CLASSES: Record<Variant, string> = {
  primary: "bg-azul text-white px-4 py-2 hover:bg-azul-oscuro",
  secondary: "bg-white text-azul border border-azul px-4 py-2 hover:bg-azul-claro",
  danger: "bg-rojo text-white px-4 py-2 hover:brightness-90",
  link: "bg-transparent text-azul px-1.5 py-0.5 hover:bg-azul-claro",
  "link-danger": "bg-transparent text-rojo px-1.5 py-0.5 hover:bg-rojo-claro",
};

export function Button({ variant = "primary", icon, children, className = "", ...rest }: ButtonProps) {
  return (
    <button className={`${BASE} ${VARIANT_CLASSES[variant]} ${className}`} {...rest}>
      {icon}
      {children}
    </button>
  );
}
