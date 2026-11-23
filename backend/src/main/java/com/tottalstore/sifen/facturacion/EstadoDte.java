package com.tottalstore.sifen.facturacion;

/** Estados del ciclo de vida del DTE (diagrama de estados de la etapa de diseño). */
public enum EstadoDte {
    BORRADOR,
    EN_VALIDACION,
    FIRMADO,
    ENVIADO_SIFEN,
    APROBADO,
    RECHAZADO,
    EN_PROCESO,
    EN_CORRECCION,
    ANULADO,
    /** Cancelado por el emisor con el evento de cancelación del SIFEN (11.5.1): sin efecto fiscal. */
    CANCELADO,
    /** DE rechazado cuyo número se inutilizó en el SIFEN con el evento de inutilización: no se reenvía. */
    INUTILIZADO
}
