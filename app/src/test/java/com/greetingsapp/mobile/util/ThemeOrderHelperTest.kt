package com.greetingsapp.mobile.util

import com.greetingsapp.mobile.data.model.ThemeModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Tests unitarios para ThemeOrderHelper.
 * Verifica la lógica de ordenamiento de temas según el día de la semana.
 */
class ThemeOrderHelperTest {

    // ==========================================
    // DATOS DE PRUEBA
    // ==========================================

    private val sampleThemes = listOf(
        ThemeModel(1, "Buenos Días"),
        ThemeModel(2, "Feliz Lunes"),
        ThemeModel(3, "Feliz Martes"),
        ThemeModel(4, "Feliz Miércoles"),
        ThemeModel(5, "Feliz Jueves"),
        ThemeModel(6, "Feliz Viernes"),
        ThemeModel(7, "Feliz Fin de Semana")
    )

    // ==========================================
    // TESTS PARA getThemeNameForDay
    // ==========================================

    @Test
    fun `lunes debe retornar Feliz Lunes`() {
        val monday = LocalDate.of(2026, 1, 26) // Lunes
        val result = ThemeOrderHelper.getThemeNameForDay(monday)
        assertEquals("Feliz Lunes", result)
    }

    @Test
    fun `martes debe retornar Feliz Martes`() {
        val tuesday = LocalDate.of(2026, 1, 27) // Martes
        val result = ThemeOrderHelper.getThemeNameForDay(tuesday)
        assertEquals("Feliz Martes", result)
    }

    @Test
    fun `miercoles debe retornar Feliz Miercoles`() {
        val wednesday = LocalDate.of(2026, 1, 28) // Miércoles
        val result = ThemeOrderHelper.getThemeNameForDay(wednesday)
        assertEquals("Feliz Miércoles", result)
    }

    @Test
    fun `jueves debe retornar Feliz Jueves`() {
        val thursday = LocalDate.of(2026, 1, 29) // Jueves
        val result = ThemeOrderHelper.getThemeNameForDay(thursday)
        assertEquals("Feliz Jueves", result)
    }

    @Test
    fun `viernes debe retornar Feliz Viernes`() {
        val friday = LocalDate.of(2026, 1, 30) // Viernes
        val result = ThemeOrderHelper.getThemeNameForDay(friday)
        assertEquals("Feliz Viernes", result)
    }

    @Test
    fun `sabado debe retornar Feliz Fin de Semana`() {
        val saturday = LocalDate.of(2026, 1, 31) // Sábado
        val result = ThemeOrderHelper.getThemeNameForDay(saturday)
        assertEquals("Feliz Fin de Semana", result)
    }

    @Test
    fun `domingo debe retornar Feliz Fin de Semana`() {
        val sunday = LocalDate.of(2026, 2, 1) // Domingo
        val result = ThemeOrderHelper.getThemeNameForDay(sunday)
        assertEquals("Feliz Fin de Semana", result)
    }

    // ==========================================
    // TESTS PARA reorderThemesWithTodayFirst
    // ==========================================

    @Test
    fun `reordenar con tema Feliz Lunes debe ponerlo primero`() {
        val result = ThemeOrderHelper.reorderThemesWithTodayFirst(
            themes = sampleThemes,
            todayThemeName = "Feliz Lunes"
        )

        assertEquals("Feliz Lunes", result.first().themeName)
        assertEquals(sampleThemes.size, result.size)
    }

    @Test
    fun `reordenar con tema Feliz Viernes debe ponerlo primero`() {
        val result = ThemeOrderHelper.reorderThemesWithTodayFirst(
            themes = sampleThemes,
            todayThemeName = "Feliz Viernes"
        )

        assertEquals("Feliz Viernes", result.first().themeName)
        assertEquals(sampleThemes.size, result.size)
    }

    @Test
    fun `reordenar con tema Feliz Fin de Semana debe ponerlo primero`() {
        val result = ThemeOrderHelper.reorderThemesWithTodayFirst(
            themes = sampleThemes,
            todayThemeName = "Feliz Fin de Semana"
        )

        assertEquals("Feliz Fin de Semana", result.first().themeName)
        assertEquals(sampleThemes.size, result.size)
    }

    @Test
    fun `reordenar debe mantener todos los elementos sin duplicados`() {
        val result = ThemeOrderHelper.reorderThemesWithTodayFirst(
            themes = sampleThemes,
            todayThemeName = "Feliz Miércoles"
        )

        // Verificar que no hay duplicados
        val uniqueIds = result.map { it.themeId }.toSet()
        assertEquals(sampleThemes.size, uniqueIds.size)

        // Verificar que todos los IDs originales están presentes
        val originalIds = sampleThemes.map { it.themeId }.toSet()
        assertEquals(originalIds, uniqueIds)
    }

    @Test
    fun `reordenar con tema inexistente debe mantener orden original`() {
        val result = ThemeOrderHelper.reorderThemesWithTodayFirst(
            themes = sampleThemes,
            todayThemeName = "Tema Que No Existe"
        )

        assertEquals(sampleThemes, result)
    }

    @Test
    fun `reordenar ignora mayusculas y minusculas`() {
        val result = ThemeOrderHelper.reorderThemesWithTodayFirst(
            themes = sampleThemes,
            todayThemeName = "FELIZ LUNES" // Todo mayúsculas
        )

        assertEquals("Feliz Lunes", result.first().themeName)
    }

    @Test
    fun `reordenar lista vacia debe retornar lista vacia`() {
        val result = ThemeOrderHelper.reorderThemesWithTodayFirst(
            themes = emptyList(),
            todayThemeName = "Feliz Lunes"
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun `reordenar lista con un solo elemento debe retornar el mismo elemento`() {
        val singleTheme = listOf(ThemeModel(1, "Feliz Lunes"))

        val result = ThemeOrderHelper.reorderThemesWithTodayFirst(
            themes = singleTheme,
            todayThemeName = "Feliz Lunes"
        )

        assertEquals(1, result.size)
        assertEquals("Feliz Lunes", result.first().themeName)
    }

    @Test
    fun `tema del dia no debe aparecer duplicado en la lista`() {
        val result = ThemeOrderHelper.reorderThemesWithTodayFirst(
            themes = sampleThemes,
            todayThemeName = "Feliz Jueves"
        )

        val juevesCount = result.count { it.themeName == "Feliz Jueves" }
        assertEquals(1, juevesCount)
    }
}
