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

export interface PuntoExpedicion {
  id: string;
  codigo: string;
  descripcion: string | null;
  activo: boolean;
}

export interface Establecimiento {
  id: string;
  codigo: string;
  denominacion: string;
  direccion: string | null;
  numeroCasa: string | null;
  departamentoCodigo: string | null;
  departamentoDescripcion: string | null;
  distritoCodigo: string | null;
  distritoDescripcion: string | null;
  ciudadCodigo: string | null;
  ciudadDescripcion: string | null;
  telefono: string | null;
  email: string | null;
  activo: boolean;
  puntosExpedicion: PuntoExpedicion[];
}

export interface Factura {
  id: string;
  estadoDte: EstadoDte;
  clienteRuc: string;
  clienteRazonSocial: string;
  establecimientoId: string;
  establecimientoCodigo: string;
  establecimientoDenominacion: string;
  puntoExpedicionId: string;
  puntoExpedicionCodigo: string;
  puntoExpedicionDescripcion: string | null;
  condicionPago: CondicionPago;
  plazoDias: number | null;
  cantidadCuotas: number | null;
  totalIva5: number;
  totalIva10: number;
  totalGeneral: number;
  fechaEmision: string;
  items: ItemFactura[];
  motivoRechazo: string | null;
  emailEnviado: boolean;
  /** 001-001-0000001; null mientras la factura nunca se confirmó. */
  numero: string | null;
  cdc: string | null;
}

export interface RechazoResumen {
  facturaId: string;
  clienteRazonSocial: string;
  motivo: string | null;
  fechaEmision: string;
}

export interface ResumenDashboard {
  facturasPorEstado: Partial<Record<EstadoDte, number>>;
  cantidadFacturasMes: number;
  totalFacturadoMes: number;
  ultimosRechazados: RechazoResumen[];
}

export interface AuditoriaEntrada {
  id: string;
  operacion: string;
  fechaHora: string;
  usuarioNombre: string | null;
  facturaId: string | null;
}

export interface PaginaSpring<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  number: number;
  size: number;
}

export interface ConfiguracionEmisor {
  razonSocial: string | null;
  nombreFantasia: string | null;
  direccion: string | null;
  numeroCasa: string | null;
  complementoDireccion1: string | null;
  complementoDireccion2: string | null;
  departamentoCodigo: string | null;
  departamentoDescripcion: string | null;
  distritoCodigo: string | null;
  distritoDescripcion: string | null;
  ciudadCodigo: string | null;
  ciudadDescripcion: string | null;
  telefono: string | null;
  email: string | null;
  actividadEconomicaCodigo: string | null;
  actividadEconomicaDescripcion: string | null;
  rucBase: string | null;
  dvRuc: number | null;
  tipoContribuyente: number | null;
  timbradoNumero: string | null;
  timbradoFechaInicio: string | null;
  timbradoFechaFin: string | null;
}

export interface RenglonLibroIva {
  facturaId: string;
  fecha: string;
  tipoComprobante: string;
  numero: string;
  timbrado: string | null;
  cdc: string | null;
  clienteRuc: string;
  clienteRazonSocial: string;
  condicion: string;
  gravado10: number;
  iva10: number;
  gravado5: number;
  iva5: number;
  exento: number;
  total: number;
}

export interface LibroIva {
  periodo: string;
  emisorRuc: string | null;
  emisorRazonSocial: string | null;
  renglones: RenglonLibroIva[];
  totales: {
    cantidadComprobantes: number;
    gravado10: number;
    iva10: number;
    baseImponible10: number;
    gravado5: number;
    iva5: number;
    baseImponible5: number;
    exento: number;
    totalIva: number;
    total: number;
  };
}

export interface ReporteRechazos {
  desde: string;
  hasta: string;
  totalIntentos: number;
  aprobados: number;
  rechazados: number;
  tasaAprobacion: number | null;
  facturasConRechazo: number;
  porCodigo: { codigo: string; mensaje: string | null; cantidad: number; ultimaVez: string }[];
  porMes: { mes: string; intentos: number; aprobados: number; rechazados: number }[];
  ultimosRechazos: {
    facturaId: string;
    fecha: string;
    clienteRuc: string;
    clienteRazonSocial: string;
    codigo: string | null;
    mensaje: string | null;
  }[];
}

export interface HistorialCliente {
  ruc: string;
  razonSocial: string;
  cantidadFacturas: number;
  aprobadas: number;
  rechazadas: number;
  pendientes: number;
  montoAprobado: number;
  intentosEnvio: number;
  intentosRechazados: number;
  ultimaFactura: string | null;
  facturas: {
    facturaId: string;
    fechaEmision: string;
    numero: string;
    estado: EstadoDte;
    total: number;
    intentos: number;
    ultimoCodigo: string | null;
    ultimoMensaje: string | null;
  }[];
}
