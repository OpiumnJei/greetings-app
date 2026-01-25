package com.greetingsapp.mobile.ui.utils

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.greetingsapp.mobile.util.SpanCalculator

/**
 * Calcula cuántas columnas caben en la pantalla.
 * @param minWidthDp El ancho mínimo deseado para cada tarjeta (ej. 160dp).
 * @return El número de columnas (mínimo 2).
 *
 * Ejemplos con 160dp como ancho base:
 * - Celular normal (360dp ancho) -> 2 Columnas
 * - Tablet (600dp ancho) -> 3 Columnas
 * - Tablet Horizontal (900dp ancho) -> 5 Columnas
 */
fun Context.calculateDynamicSpanCount(minWidthDp: Int = 160): Int {

    // 1. Obtenemos las métricas de la pantalla (pixeles, densidad, etc.)
    val displayMetrics = resources.displayMetrics

    // 2. Calculamos el ancho de la pantalla en DP (Density-independent Pixels)
    // Fórmula: Pixeles / Densidad = DP
    val screenWidthDp = displayMetrics.widthPixels / displayMetrics.density

    // Delegamos la lógica pura al objeto utilitario (testeable)
    return SpanCalculator.calculateSpanCount(screenWidthDp, minWidthDp)
}

// muestra el teclado
fun View.showKeyboard() {
    this.requestFocus()
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
}

// oculta el teclado
fun View.hideKeyboard() {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(windowToken, 0)
    clearFocus()
}

