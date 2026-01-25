package com.greetingsapp.mobile.util

/**
 * Objeto utilitario para calcular el número de columnas de un GridLayout.
 * Extraído como función pura para facilitar los tests unitarios.
 */
object SpanCalculator {

    /**
     * Calcula cuántas columnas caben en la pantalla.
     * @param screenWidthDp El ancho de la pantalla en DP.
     * @param minItemWidthDp El ancho mínimo deseado para cada tarjeta (ej. 160dp).
     * @return El número de columnas (mínimo 2).
     *
     * Ejemplos:
     * - Celular normal (360dp ancho) -> 360/160 = 2.25 -> 2 Columnas
     * - Tablet (600dp ancho) -> 600/160 = 3.75 -> 3 Columnas
     * - Tablet Horizontal (900dp ancho) -> 900/160 = 5.6 -> 5 Columnas
     */
    fun calculateSpanCount(screenWidthDp: Float, minItemWidthDp: Int): Int {
        // Dividimos el ancho total entre el ancho de la tarjeta(item)
        val noOfColumns = (screenWidthDp / minItemWidthDp).toInt()

        // Retornamos el resultado, pero aseguramos que MÍNIMO haya 2 columnas
        if (noOfColumns >= 2) {
            return noOfColumns
        } else {
            return 2
        }
    }
}
