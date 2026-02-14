package com.greetingsapp.mobile.util

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests unitarios para SpanCalculator.
 * Verifica la lógica de cálculo de columnas para diferentes tamaños de pantalla.
 */
class SpanCalculatorTest {

    // ==========================================
    // TESTS PARA DISPOSITIVOS COMUNES
    // ==========================================

    @Test
    fun `celular pequeno 320dp debe retornar 2 columnas`() {
        val result = SpanCalculator.calculateSpanCount(
            screenWidthDp = 320f,
            minItemWidthDp = 160
        )
        assertEquals(2, result)
    }

    @Test
    fun `celular normal 360dp debe retornar 2 columnas`() {
        val result = SpanCalculator.calculateSpanCount(
            screenWidthDp = 360f,
            minItemWidthDp = 160
        )
        assertEquals(2, result)
    }

    @Test
    fun `celular grande 400dp debe retornar 2 columnas`() {
        val result = SpanCalculator.calculateSpanCount(
            screenWidthDp = 400f,
            minItemWidthDp = 160
        )
        assertEquals(2, result)
    }

    @Test
    fun `tablet vertical 600dp debe retornar 3 columnas`() {
        val result = SpanCalculator.calculateSpanCount(
            screenWidthDp = 600f,
            minItemWidthDp = 160
        )
        assertEquals(3, result)
    }

    @Test
    fun `tablet horizontal 900dp debe retornar 5 columnas`() {
        val result = SpanCalculator.calculateSpanCount(
            screenWidthDp = 900f,
            minItemWidthDp = 160
        )
        assertEquals(5, result)
    }

    @Test
    fun `tablet grande 1200dp debe retornar 7 columnas`() {
        val result = SpanCalculator.calculateSpanCount(
            screenWidthDp = 1200f,
            minItemWidthDp = 160
        )
        assertEquals(7, result)
    }

    // ==========================================
    // TESTS PARA CASOS BORDE (EDGE CASES)
    // ==========================================

    @Test
    fun `pantalla muy pequena debe retornar minimo 2 columnas`() {
        val result = SpanCalculator.calculateSpanCount(
            screenWidthDp = 100f,
            minItemWidthDp = 160
        )
        assertEquals(2, result)
    }

    @Test
    fun `ancho cero debe retornar 2 columnas por defecto`() {
        val result = SpanCalculator.calculateSpanCount(
            screenWidthDp = 0f,
            minItemWidthDp = 160
        )
        assertEquals(2, result)
    }

    @Test
    fun `ancho negativo debe retornar 2 columnas por defecto`() {
        val result = SpanCalculator.calculateSpanCount(
            screenWidthDp = -100f,
            minItemWidthDp = 160
        )
        assertEquals(2, result)
    }

    @Test
    fun `minItemWidth cero debe retornar 2 columnas por defecto`() {
        val result = SpanCalculator.calculateSpanCount(
            screenWidthDp = 360f,
            minItemWidthDp = 0
        )
        assertEquals(2, result)
    }

    @Test
    fun `minItemWidth negativo debe retornar 2 columnas por defecto`() {
        val result = SpanCalculator.calculateSpanCount(
            screenWidthDp = 360f,
            minItemWidthDp = -160
        )
        assertEquals(2, result)
    }

    // ==========================================
    // TESTS CON DIFERENTES ANCHOS DE ITEM
    // ==========================================

    @Test
    fun `celular 360dp con items de 120dp debe retornar 3 columnas`() {
        val result = SpanCalculator.calculateSpanCount(
            screenWidthDp = 360f,
            minItemWidthDp = 120
        )
        assertEquals(3, result)
    }

    @Test
    fun `celular 360dp con items de 180dp debe retornar 2 columnas`() {
        val result = SpanCalculator.calculateSpanCount(
            screenWidthDp = 360f,
            minItemWidthDp = 180
        )
        assertEquals(2, result)
    }
}
