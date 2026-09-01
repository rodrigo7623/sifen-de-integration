export type TasaIva = "EXENTA" | "CINCO" | "DIEZ";

export const TASA_IVA_LABEL: Record<TasaIva, string> = {
  EXENTA: "Exenta (0%)",
  CINCO: "5%",
  DIEZ: "10%",
};

export type Rol = "ADMIN" | "OPERARIO";

export const ROL_LABEL: Record<Rol, string> = {
  ADMIN: "Administrador",
  OPERARIO: "Operario",
};

export interface Usuario {
  id: string;
  nombre: string;
  email: string;
  rol: Rol;
  activo: boolean;
}

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

export type EstadoDte =
  | "BORRADOR"
  | "EN_VALIDACION"
  | "FIRMADO"
  | "ENVIADO_SIFEN"
  | "APROBADO"
  | "RECHAZADO"
  | "EN_PROCESO"
  | "EN_CORRECCION"
  | "ANULADO";

export const ESTADO_DTE_LABEL: Record<EstadoDte, string> = {
  BORRADOR: "Borrador",
  EN_VALIDACION: "En validación",
  FIRMADO: "Firmado",
  ENVIADO_SIFEN: "Enviado al SIFEN",
  APROBADO: "Aprobado",
  RECHAZADO: "Rechazado",
  EN_PROCESO: "En proceso",
  EN_CORRECCION: "En corrección",
  ANULADO: "Anulado",
};

export type CondicionPago = "CONTADO" | "CREDITO";

export const CONDICION_PAGO_LABEL: Record<CondicionPago, string> = {
  CONTADO: "Contado",
  CREDITO: "Crédito",
};

export interface ItemFactura {
  productoCodigo: string | null;
  descripcion: string;
  cantidad: number;
  precioUnitario: number;
  tasaIva: TasaIva;
  subtotal: number;
}

export interface Factura {
  id: string;
  estadoDte: EstadoDte;
  clienteRuc: string;
  clienteRazonSocial: string;
  condicionPago: CondicionPago;
  plazoDias: number | null;
  cantidadCuotas: number | null;
  totalIva5: number;
  totalIva10: number;
  totalGeneral: number;
  fechaEmision: string;
  items: ItemFactura[];
}
