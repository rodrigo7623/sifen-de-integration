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
    private final EnviadorSifenService enviadorSifenService;
    private final EnviadorSifenConfigRepository enviadorSifenConfigRepository;

    public SifenDiagnosticoController(
            ObjectProvider<ConsultaRucSifenService> consultaRucSifenService,
            EnviadorSifenService enviadorSifenService,
            EnviadorSifenConfigRepository enviadorSifenConfigRepository) {
        this.consultaRucSifenService = consultaRucSifenService;
        this.enviadorSifenService = enviadorSifenService;
        this.enviadorSifenConfigRepository = enviadorSifenConfigRepository;
    }

    /** Consulta un DE por CDC (siConsDE): permite confirmar en el SIFEN cualquier documento emitido. */
    @GetMapping("/consulta-de/{cdc}")
    public RespuestaSifenResult consultarDe(@PathVariable String cdc) {
        if (!cdc.matches("\\d{44}")) {
            throw new IllegalArgumentException("El CDC debe tener 44 dígitos");
        }
        Ambiente ambiente = enviadorSifenConfigRepository.findAll().stream()
                .findFirst()
                .map(EnviadorSifenConfig::getAmbiente)
                .orElse(Ambiente.TEST);
        return enviadorSifenService.consultarDe(null, cdc, ambiente);
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
