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

/** C002 del Manual Técnico: factura, nota de crédito, nota de débito. */
export type TipoDocumento = "FE" | "NC" | "ND";

export const TIPO_DOCUMENTO_LABEL: Record<TipoDocumento, string> = {
  FE: "Factura",
  NC: "Nota de crédito",
  ND: "Nota de débito",
};

/** E401 iMotEmi. */
export type MotivoNota =
  | "DEVOLUCION_Y_AJUSTE_PRECIOS"
  | "DEVOLUCION"
  | "DESCUENTO"
  | "BONIFICACION"
  | "CREDITO_INCOBRABLE"
  | "RECUPERO_COSTO"
  | "RECUPERO_GASTO"
  | "AJUSTE_PRECIO";

export const MOTIVO_NOTA_LABEL: Record<MotivoNota, string> = {
  DEVOLUCION_Y_AJUSTE_PRECIOS: "Devolución y ajuste de precios",
  DEVOLUCION: "Devolución",
  DESCUENTO: "Descuento",
  BONIFICACION: "Bonificación",
  CREDITO_INCOBRABLE: "Crédito incobrable",
  RECUPERO_COSTO: "Recupero de costo",
  RECUPERO_GASTO: "Recupero de gasto",
  AJUSTE_PRECIO: "Ajuste de precio",
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
  /** null en notas de crédito/débito (no informan condición de la operación). */
  condicionPago: CondicionPago | null;
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
  tipoDocumento: TipoDocumento;
  /** Solo en notas de crédito/débito. */
  motivoEmision: MotivoNota | null;
  facturaAsociadaId: string | null;
  facturaAsociadaNumero: string | null;
  facturaAsociadaCdc: string | null;
  facturaAsociadaTotal: number | null;
  /** RF-09: a dónde se manda el documento aprobado; null si el cliente no tiene email. */
  clienteEmail: string | null;
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
  usuarioEmail: string | null;
  usuarioRol: Rol | null;
  facturaId: string | null;
  documentoTipo: TipoDocumento | null;
  documentoNumero: string | null;
  /** Estado actual del documento (no el que tenía al registrarse la operación). */
  documentoEstado: EstadoDte | null;
  clienteRuc: string | null;
  clienteRazonSocial: string | null;
  documentoTotal: number | null;
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
  /** D2.1 gActEco: hasta 9, exactamente una principal. */
  actividadesEconomicas: ActividadEconomica[];
}

export interface ActividadEconomica {
  codigo: string;
  descripcion: string;
  principal: boolean;
}

export type TipoLibro = "VENTAS" | "COMPRAS";

/** SIFEN = DE propio aprobado; REGISTRADO = comprobante cargado a mano; DE_RECIBIDO = compra con CDC. */
export type OrigenRenglon = "SIFEN" | "REGISTRADO" | "DE_RECIBIDO";

export interface RenglonLibroIva {
  facturaId: string | null;
  comprobanteRegistradoId: string | null;
  origen: OrigenRenglon;
  fecha: string;
  tipoComprobante: string;
  sigla: string;
  numero: string;
  timbrado: string | null;
  cdc: string | null;
  ruc: string;
  razonSocial: string;
  condicion: string;
  documentoAsociado: string | null;
  gravado10: number;
  iva10: number;
  gravado5: number;
  iva5: number;
  exento: number;
  total: number;
}

export interface LiquidacionIva {
  periodo: string;
  comprobantesVenta: number;
  totalVentas: number;
  debitoFiscal10: number;
  debitoFiscal5: number;
  debitoFiscal: number;
  comprobantesCompra: number;
  totalCompras: number;
  creditoFiscal10: number;
  creditoFiscal5: number;
  creditoFiscal: number;
  saldo: number;
  comprobantesParaRg90: number;
  comprobantesElectronicosExcluidos: number;
}

export type TipoRegistro = "VENTA" | "COMPRA";

export interface TipoComprobanteRg90 {
  valor: string;
  codigo: number;
  descripcion: string;
  sigla: string;
  ventas: boolean;
  compras: boolean;
  soloTotalEnCompras: boolean;
  numeroOpcional: boolean;
  sinTimbrado: boolean;
  esNota: boolean;
  exigeRucEnCompras: boolean;
}

export interface TipoIdentificacionRg90 {
  valor: string;
  codigo: number;
  descripcion: string;
  nombreOpcional: boolean;
}

export interface CatalogoRg90 {
  tiposComprobante: TipoComprobanteRg90[];
  tiposIdentificacion: TipoIdentificacionRg90[];
}

export interface ComprobanteRegistradoInput {
  tipoRegistro: TipoRegistro;
  tipoComprobante: string;
  fechaEmision: string;
  tipoIdentificacion: string;
  identificacion: string;
  razonSocial: string | null;
  timbrado: string | null;
  numero: string | null;
  cdc: string | null;
  condicion: "CONTADO" | "CREDITO";
  monedaExtranjera: boolean;
  gravado10: number;
  gravado5: number;
  exento: number;
  total: number;
  imputaIva: boolean;
  imputaIre: boolean;
  imputaIrp: boolean;
  noImputa: boolean;
  asociadoNumero: string | null;
  asociadoTimbrado: string | null;
  observacion: string | null;
}

export interface ComprobanteRegistrado extends ComprobanteRegistradoInput {
  id: string;
  tipoComprobanteDescripcion: string;
  electronico: boolean;
  iva10: number;
  iva5: number;
  creadoPor: string | null;
  modificadoPor: string | null;
  updatedAt: string;
}

export interface LibroIva {
  libro: TipoLibro;
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

/** GET /clientes/consulta-ruc: el cliente ya registrado o los datos del SIFEN (siConsRUC). */
export interface ConsultaRucCliente {
  /** Normalizado base-DV; el DV se calcula si se ingresó solo el número. */
  ruc: string;
  dvCalculado: boolean;
  cliente: Cliente | null;
  consultadoEnSifen: boolean;
  /** true: figura en el SIFEN; false: no figura; null: no se pudo consultar. */
  existeEnSifen: boolean | null;
  razonSocial: string | null;
  estado: string | null;
  facturadorElectronico: boolean | null;
  condicionIvaSugerida: CondicionIva | null;
  mensaje: string;
}
