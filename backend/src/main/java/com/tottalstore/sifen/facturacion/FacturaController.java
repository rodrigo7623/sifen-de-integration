package com.tottalstore.sifen.facturacion;

import com.tottalstore.sifen.facturacion.dto.FacturaRequest;
import com.tottalstore.sifen.facturacion.dto.FacturaResponse;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/facturas")
public class FacturaController {

    private final FacturaService facturaService;
    private final FacturaPdfGenerator facturaPdfGenerator;

    public FacturaController(FacturaService facturaService, FacturaPdfGenerator facturaPdfGenerator) {
        this.facturaService = facturaService;
        this.facturaPdfGenerator = facturaPdfGenerator;
    }

    /** Facturas y notas de crédito/débito; {@code tipo} filtra por tipo de documento. */
    @GetMapping
    public List<FacturaResponse> listar(
            @RequestParam(required = false) EstadoDte estado, @RequestParam(required = false) TipoDocumento tipo) {
        return facturaService.listar(estado, tipo).stream().map(FacturaResponse::from).toList();
    }

    @GetMapping("/{id}")
    public FacturaResponse obtener(@PathVariable UUID id) {
        return FacturaResponse.from(facturaService.obtener(id));
    }

    /** M6: notas de crédito/débito emitidas sobre la factura. */
    @GetMapping("/{id}/notas")
    public List<FacturaResponse> notas(@PathVariable UUID id) {
        return facturaService.notasDe(id).stream().map(FacturaResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FacturaResponse crearBorrador(@Valid @RequestBody FacturaRequest request) {
        return FacturaResponse.from(facturaService.crearBorrador(request));
    }

    @PutMapping("/{id}")
    public FacturaResponse editarBorrador(@PathVariable UUID id, @Valid @RequestBody FacturaRequest request) {
        return FacturaResponse.from(facturaService.editarBorrador(id, request));
    }

    @PostMapping("/{id}/confirmar")
    public FacturaResponse confirmar(@PathVariable UUID id) {
        return FacturaResponse.from(facturaService.confirmarEnvio(id));
    }

    /** Resuelve un documento EN_PROCESO (el SIFEN no respondió al enviarlo) consultándolo por CDC. */
    /** RF-09: vuelve a mandar por correo un documento aprobado (p. ej. después de cargarle el email al cliente). */
    @PostMapping("/{id}/reenviar-correo")
    public FacturaResponse reenviarCorreo(@PathVariable UUID id) {
        return FacturaResponse.from(facturaService.reenviarCorreo(id));
    }

    @PostMapping("/{id}/verificar")
    public FacturaResponse verificar(@PathVariable UUID id) {
        return FacturaResponse.from(facturaService.verificarEnvio(id));
    }

    @PostMapping("/{id}/reabrir")
    public FacturaResponse reabrir(@PathVariable UUID id) {
        return FacturaResponse.from(facturaService.reabrir(id));
    }

    @GetMapping("/{id}/xml")
    public ResponseEntity<byte[]> descargarXml(@PathVariable UUID id) {
        FacturaElectronica factura = facturaService.obtener(id);
        String xml = facturaService.obtenerXmlFirmado(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .header("Content-Disposition", "attachment; filename=\"" + nombreArchivo(factura) + ".xml\"")
                .body(xml.getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> descargarPdf(@PathVariable UUID id) {
        FacturaElectronica factura = facturaService.obtener(id);
        byte[] pdf = facturaPdfGenerator.generar(factura);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header("Content-Disposition", "attachment; filename=\"" + nombreArchivo(factura) + ".pdf\"")
                .body(pdf);
    }

    /** factura-001-001-0000001 (nota-credito-…, nota-debito-…); con el id si todavía no tiene número. */
    static String nombreArchivo(FacturaElectronica factura) {
        String numero = factura.numeroComprobante();
        String prefijo = switch (factura.getTipoDoc()) {
            case NC -> "nota-credito-";
            case ND -> "nota-debito-";
            default -> "factura-";
        };
        return prefijo + (numero != null ? numero : factura.getId());
    }
}
