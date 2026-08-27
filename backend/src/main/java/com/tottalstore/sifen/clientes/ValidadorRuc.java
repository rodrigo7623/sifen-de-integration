package com.tottalstore.sifen.clientes;

/**
 * Punto de extensión para la validación del RUC (RF-15, CU-12).
 *
 * <p>La implementación real debería consultar el padrón de contribuyentes de la SET. Mientras no
 * se cuente con certificado y acceso a ese servicio, se usa {@link ValidadorRucStub}, que solo
 * valida el formato. Sustituir la implementación aquí no requiere cambios en {@code ClienteService}
 * ni en los controladores (RNF-09).
 */
public interface ValidadorRuc {

    ResultadoValidacionRuc validar(String ruc);
}
