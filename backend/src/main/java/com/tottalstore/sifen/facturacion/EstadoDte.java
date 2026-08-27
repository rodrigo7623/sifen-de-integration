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
    ANULADO
}
