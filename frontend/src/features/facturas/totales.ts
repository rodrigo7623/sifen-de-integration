import type { TasaIva } from "../../api/types";

const FRACCION_IVA: Record<TasaIva, number> = { EXENTA: 0, CINCO: 0.05, DIEZ: 0.1 };

export interface ItemParaTotales {
  cantidad: number;
  precioUnitario: number;
  tasaIva: TasaIva;
}

export interface Totales {
  totalIva5: number;
  totalIva10: number;
  totalGeneral: number;
}

/**
 * Réplica en el cliente de FacturaService.calcularTotales/calcularSubtotalItem (backend), para
 * mostrar el total en vivo mientras se arma la factura. La fuente de verdad sigue siendo el
 * backend: estos números se recalculan del lado del servidor al guardar.
 */
export function calcularTotales(items: ItemParaTotales[]): Totales {
  let totalIva5 = 0;
  let totalIva10 = 0;
  let totalGeneral = 0;

  for (const item of items) {
    const itemTotal = Math.round((item.cantidad || 0) * (item.precioUnitario || 0));
    totalGeneral += itemTotal;

    if (item.tasaIva === "EXENTA") continue;

    const base = Math.round(itemTotal / (1 + FRACCION_IVA[item.tasaIva]));
    const iva = itemTotal - base;

    if (item.tasaIva === "CINCO") totalIva5 += iva;
    else totalIva10 += iva;
  }

  return { totalIva5, totalIva10, totalGeneral };
}

export function subtotalItem(item: ItemParaTotales): number {
  return Math.round((item.cantidad || 0) * (item.precioUnitario || 0));
}
