package com.greetingsapp.mobile.ui.utils

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager

/**
 * Calcula cuántas columnas caben en la pantalla.
 * @param minWidthDp El ancho mínimo deseado para cada tarjeta (ej. 160dp).
 * @return El número de columnas (mínimo 2).
 *
 *          // Usamos 160dp como ancho base.
 *          // - Celular normal (360dp ancho) -> 360/160 = 2.25 -> 2 Columnas
 *          // - Tablet (600dp ancho) -> 600/160 = 3.75 -> 3 Columnas
 *          // - Tablet Horizontal (900dp ancho) -> 900/160 = 5.6 -> 5 Columnas
 */
fun Context.calculateDynamicSpanCount(minWidthDp: Int = 160): Int {
    // 1. Obtenemos las métricas de la pantalla (pixeles, densidad, etc.)
    val displayMetrics = resources.displayMetrics

    // 2. Calculamos el ancho de la pantalla en DP (Density-independent Pixels)
    // Fórmula: Pixeles / Densidad = DP
    val screenWidthDp = displayMetrics.widthPixels / displayMetrics.density

    // 3. Dividimos el ancho total entre el ancho de tu tarjeta
    val noOfColumns = (screenWidthDp / minWidthDp).toInt()

    // 4. Retornamos el resultado, pero aseguramos que MÍNIMO haya 2 columnas
    if (noOfColumns >= 2) {
        return noOfColumns
    } else {
        return 2
    }
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

