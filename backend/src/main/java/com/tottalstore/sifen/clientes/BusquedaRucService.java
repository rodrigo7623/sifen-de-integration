package com.tottalstore.sifen.clientes;

import com.tottalstore.sifen.clientes.dto.ClienteResponse;
import com.tottalstore.sifen.clientes.dto.ConsultaRucClienteResponse;
import com.tottalstore.sifen.common.BusinessException;
import com.tottalstore.sifen.shared.CondicionIva;
import com.tottalstore.sifen.shared.Modulo11;
import com.tottalstore.sifen.sifen.ConsultaRucResult;
import com.tottalstore.sifen.sifen.ConsultaRucSifenService;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * Busca un RUC para el alta de clientes y la carga de facturas (RF-15, CU-12): primero entre los
 * clientes registrados y, si no está, en el SIFEN ({@code siConsRUC}, Manual Técnico v150, 9.6) para
 * precargar la razón social.
 *
 * <p>La consulta al SIFEN es una ayuda, no un requisito: si no está disponible (modo stub, ambiente
 * de test caído) o el RUC no figura (p. ej. un consumidor final con su cédula), se informa y el alta
 * sigue con la validación local del dígito verificador.
 */
@Service
public class BusquedaRucService {

    private static final Pattern SOLO_NUMERO = Pattern.compile("^\\d{4,8}$");
    private static final Pattern CON_DV = Pattern.compile("^\\d{4,8}-\\d$");
    private static final String RUC_ENCONTRADO = "0502";
    private static final String RUC_INEXISTENTE = "0500";

    private final ClienteRepository clienteRepository;
    private final ObjectProvider<ConsultaRucSifenService> consultaRucSifenService;

    public BusquedaRucService(
            ClienteRepository clienteRepository, ObjectProvider<ConsultaRucSifenService> consultaRucSifenService) {
        this.clienteRepository = clienteRepository;
        this.consultaRucSifenService = consultaRucSifenService;
    }

    public ConsultaRucClienteResponse buscar(String ingresado) {
        String limpio = ingresado == null ? "" : ingresado.replaceAll("[\\s.]", "");
        boolean dvCalculado = SOLO_NUMERO.matcher(limpio).matches();
        String ruc = dvCalculado ? limpio + "-" + Modulo11.calcularDigitoVerificador(limpio) : limpio;
        if (!CON_DV.matcher(ruc).matches()) {
            throw new BusinessException("Formato inválido. Ingrese el número (ej. 80012345) o número y dígito verificador (80012345-6)");
        }
        String base = ruc.substring(0, ruc.indexOf('-'));
        if (Modulo11.calcularDigitoVerificador(base) != Character.getNumericValue(ruc.charAt(ruc.length() - 1))) {
            throw new BusinessException("El dígito verificador no corresponde al número base");
        }

        Optional<Cliente> registrado = clienteRepository.findById(ruc);
        if (registrado.isPresent()) {
            Cliente c = registrado.get();
            return new ConsultaRucClienteResponse(ruc, dvCalculado, ClienteResponse.from(c), false, null,
                    c.getRazonSocial(), null, null, c.getCondicionIva(),
                    c.isActivo() ? "Cliente ya registrado" : "Cliente registrado pero desactivado");
        }
        return consultarSifen(ruc, base, dvCalculado);
    }

    private ConsultaRucClienteResponse consultarSifen(String ruc, String base, boolean dvCalculado) {
        ConsultaRucSifenService servicio = consultaRucSifenService.getIfAvailable();
        if (servicio == null) {
            return sinConsulta(ruc, dvCalculado,
                    "Consulta al SIFEN no disponible en modo simulado: solo se validó el dígito verificador");
        }
        if (base.length() < 5) {
            // siConsRUC acepta de 5 a 8 caracteres (RSch03).
            return sinConsulta(ruc, dvCalculado, "El SIFEN no consulta RUC de menos de 5 dígitos");
        }

        ConsultaRucResult r = servicio.consultar(base);
        if (!r.respondioSifen()) {
            return sinConsulta(ruc, dvCalculado,
                    "No se pudo consultar al SIFEN; se validó solo el dígito verificador. Podés cargar los datos a mano");
        }
        if (RUC_ENCONTRADO.equals(r.codigo())) {
            boolean activo = r.descripcionEstado() == null || r.descripcionEstado().toUpperCase().startsWith("ACTIV");
            return new ConsultaRucClienteResponse(ruc, dvCalculado, null, true, true, recortar(r.razonSocial()),
                    r.descripcionEstado(), r.facturadorElectronico(), CondicionIva.RESPONSABLE_IVA,
                    activo ? "Datos obtenidos del SIFEN"
                            : "Atención: el contribuyente figura en el SIFEN con estado " + r.descripcionEstado());
        }
        if (RUC_INEXISTENTE.equals(r.codigo())) {
            return new ConsultaRucClienteResponse(ruc, dvCalculado, null, true, false, null, null, null,
                    CondicionIva.CONSUMIDOR_FINAL,
                    "El RUC no figura en el SIFEN. Si es un consumidor final, registralo con su cédula y cargá el nombre a mano");
        }
        return sinConsulta(ruc, dvCalculado, "El SIFEN respondió " + r.codigo() + ": " + r.mensaje());
    }

    private static ConsultaRucClienteResponse sinConsulta(String ruc, boolean dvCalculado, String mensaje) {
        return new ConsultaRucClienteResponse(ruc, dvCalculado, null, false, null, null, null, null, null, mensaje);
    }

    private static String recortar(String razonSocial) {
        return razonSocial != null ? razonSocial.trim() : null;
    }
}
