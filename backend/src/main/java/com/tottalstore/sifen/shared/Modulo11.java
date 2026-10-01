package com.tottalstore.sifen.shared;

/**
 * Algoritmo de dígito verificador módulo 11 usado por la SET tanto para el RUC como para el CDC del
 * DTE (mismo algoritmo, confirmado literalmente en el PDF oficial "Dígito Verificador" enlazado
 * desde el Manual Técnico del SIFEN v150, sección 10.2, pág. 58 — funciones de referencia en
 * PL/SQL, Visual Basic y C, todas idénticas en su lógica).
 *
 * <p>Pesos cíclicos 2 a 11, aplicados de derecha a izquierda; cualquier carácter no numérico se
 * reemplaza primero por su código ASCII (dígito a dígito) antes de aplicar los pesos — relevante
 * para RUCs de persona física que terminan en letra, no para el CDC (siempre numérico), pero se
 * implementa completo por fidelidad al algoritmo oficial.
 */
public final class Modulo11 {

    private static final int PESO_INICIAL = 2;
    private static final int PESO_MAXIMO = 11;

    private Modulo11() {}

    public static int calcularDigitoVerificador(String base) {
        if (base == null || base.isBlank()) {
            throw new IllegalArgumentException("La base para calcular el dígito verificador no puede estar vacía");
        }
        String normalizado = convertirNoDigitosAAscii(base);

        int total = 0;
        int peso = PESO_INICIAL;
        for (int i = normalizado.length() - 1; i >= 0; i--) {
            int digito = Character.getNumericValue(normalizado.charAt(i));
            total += digito * peso;
            peso = (peso == PESO_MAXIMO) ? PESO_INICIAL : peso + 1;
        }

        int resto = total % 11;
        return resto > 1 ? 11 - resto : 0;
    }

    private static String convertirNoDigitosAAscii(String base) {
        StringBuilder normalizado = new StringBuilder();
        for (int i = 0; i < base.length(); i++) {
            char c = Character.toUpperCase(base.charAt(i));
            if (c >= '0' && c <= '9') {
                normalizado.append(c);
            } else {
                normalizado.append((int) c);
            }
        }
        return normalizado.toString();
    }
}
