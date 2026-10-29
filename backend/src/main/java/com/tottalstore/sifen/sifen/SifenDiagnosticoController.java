package com.tottalstore.sifen.sifen;

import com.tottalstore.sifen.common.BusinessException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Pruebas de conectividad contra el SIFEN real. Restringido a ADMIN en {@code SecurityConfig}. Solo
 * funciona con {@code sifen.envio.modo=real}: en modo stub no hay certificado cargado.
 */
@RestController
@RequestMapping("/api/sifen")
public class SifenDiagnosticoController {

    private final ObjectProvider<ConsultaRucSifenService> consultaRucSifenService;

    public SifenDiagnosticoController(ObjectProvider<ConsultaRucSifenService> consultaRucSifenService) {
        this.consultaRucSifenService = consultaRucSifenService;
    }

    @GetMapping("/consulta-ruc/{ruc}")
    public ConsultaRucResult consultarRuc(@PathVariable String ruc) {
        ConsultaRucSifenService service = consultaRucSifenService.getIfAvailable();
        if (service == null) {
            throw new BusinessException(
                    "La consulta de RUC al SIFEN requiere SIFEN_ENVIO_MODO=real con el certificado configurado");
        }
        return service.consultar(ruc);
    }
}
