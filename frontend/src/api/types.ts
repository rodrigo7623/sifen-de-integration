export type TasaIva = "EXENTA" | "CINCO" | "DIEZ";

export const TASA_IVA_LABEL: Record<TasaIva, string> = {
  EXENTA: "Exenta (0%)",
  CINCO: "5%",
  DIEZ: "10%",
};

export type CondicionIva = "RESPONSABLE_IVA" | "PEQUENO_CONTRIBUYENTE" | "CONSUMIDOR_FINAL";

export const CONDICION_IVA_LABEL: Record<CondicionIva, string> = {
  RESPONSABLE_IVA: "Responsable IVA",
  PEQUENO_CONTRIBUYENTE: "Pequeño Contribuyente",
  CONSUMIDOR_FINAL: "Consumidor Final",
};

export interface Producto {
  codigo: string;
  descripcion: string;
  unidadMedida: string;
  precioBase: number;
  tasaIva: TasaIva;
  activo: boolean;
}

export interface Cliente {
  ruc: string;
  razonSocial: string;
  direccion: string | null;
  email: string | null;
  condicionIva: CondicionIva;
  activo: boolean;
}

export interface ResultadoValidacionRuc {
  valido: boolean;
  mensaje: string;
}
