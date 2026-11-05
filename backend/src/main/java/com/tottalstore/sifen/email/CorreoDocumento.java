package com.tottalstore.sifen.email;

import com.tottalstore.sifen.facturacion.FacturaElectronica;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Asunto, cuerpo y nombres de adjunto del correo de un documento aprobado (RF-09). Lo usan el envío
 * real y el stub, así el correo simulado muestra exactamente lo que recibiría el cliente.
 */
public record CorreoDocumento(String destinatario, String asunto, String cuerpo, String nombrePdf, String nombreXml) {

    public static CorreoDocumento de(FacturaElectronica f, String emisor) {
        String tipo = f.getTipoDoc().descripcion();
        String numero = f.numeroComprobante() != null ? f.numeroComprobante() : f.getId().toString();
        String empresa = emisor != null && !emisor.isBlank() ? emisor : "Tottal Store";
        String cliente = f.getCliente() != null ? f.getCliente().getRazonSocial() : null;
        String cdc = f.cdcVigente();

        StringBuilder cuerpo = new StringBuilder()
                .append("Hola").append(cliente != null ? " " + cliente : "").append(",\n\n")
                .append("Te enviamos tu ").append(tipo.toLowerCase(new Locale("es")))
                .append(" N.° ").append(numero).append(" de ").append(empresa)
                .append(", aprobada por el SIFEN, por un total de Gs. ").append(guaranies(f.getTotalGeneral()))
                .append(".\n\n");
        if (cdc != null) {
            cuerpo.append("CDC: ").append(cdc).append("\n")
                    .append("Podés verificarla en https://ekuatia.set.gov.py/consultas/ con ese CDC.\n\n");
        }
        cuerpo.append("Adjuntamos el KuDE (PDF) y el documento electrónico (XML).\n\n")
                .append("Gracias por tu compra.\n").append(empresa).append("\n");

        String prefijo = switch (f.getTipoDoc()) {
            case FE -> "factura";
            case NC -> "nota-credito";
            case ND -> "nota-debito";
        };
        String base = prefijo + "-" + numero;
        return new CorreoDocumento(
                f.getCliente() != null ? f.getCliente().getEmail() : null,
                tipo + " N.° " + numero + " — " + empresa,
                cuerpo.toString(),
                base + ".pdf",
                base + ".xml");
    }

    public boolean tieneDestinatario() {
        return destinatario != null && !destinatario.isBlank();
    }

    static String guaranies(BigDecimal monto) {
        NumberFormat formato = NumberFormat.getIntegerInstance(new Locale("es", "PY"));
        return formato.format(monto != null ? monto : BigDecimal.ZERO);
    }
}
