import type { ConsultaRucCliente } from "../../api/types";

export type TonoRuc = "ok" | "aviso" | "error";

export const TONO_RUC_CLASE: Record<TonoRuc, string> = {
  ok: "text-verde",
  aviso: "text-ambar",
  error: "text-rojo",
};

/** Texto para el usuario con lo que devolvió la consulta (estado y facturación electrónica). */
export function mensajeConsulta(r: ConsultaRucCliente): string {
  const partes = [r.mensaje];
  if (r.dvCalculado) partes.push(`dígito verificador calculado: ${r.ruc}`);
  if (r.existeEnSifen && r.estado) partes.push(`estado ${r.estado}`);
  if (r.existeEnSifen && r.facturadorElectronico !== null) {
    partes.push(r.facturadorElectronico ? "facturador electrónico" : "no es facturador electrónico");
  }
  return partes.join(" · ");
}
