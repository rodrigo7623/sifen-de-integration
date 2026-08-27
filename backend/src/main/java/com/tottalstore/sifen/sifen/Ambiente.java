package com.tottalstore.sifen.sifen;

/**
 * Ambiente de comunicación con el SIFEN (Actualización 1 de la planificación de Release 2:
 * atributo agregado a ENVIADOR_SIFEN para alternar de endpoint sin modificar código, RNF-09).
 * Releases 1 y 2 operan en TEST (homologación); Release 3 pasa a PRODUCCION.
 */
public enum Ambiente {
    TEST,
    PRODUCCION
}
