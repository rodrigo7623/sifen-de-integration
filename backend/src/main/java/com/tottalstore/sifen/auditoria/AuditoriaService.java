package com.tottalstore.sifen.auditoria;

import com.tottalstore.sifen.auth.Usuario;
import com.tottalstore.sifen.facturacion.FacturaElectronica;
import org.springframework.stereotype.Service;

@Service
public class AuditoriaService {

    private final LogAuditoriaRepository logAuditoriaRepository;

    public AuditoriaService(LogAuditoriaRepository logAuditoriaRepository) {
        this.logAuditoriaRepository = logAuditoriaRepository;
    }

    public void registrar(String operacion, Usuario usuario, FacturaElectronica factura) {
        LogAuditoria log = new LogAuditoria();
        log.setOperacion(operacion);
        log.setUsuario(usuario);
        log.setFactura(factura);
        logAuditoriaRepository.save(log);
    }
}
