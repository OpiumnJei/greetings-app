package com.greetingsapp.mobile.util

import com.greetingsapp.mobile.data.model.ThemeModel
import java.time.DayOfWeek
import java.time.DayOfWeek.*
import java.time.LocalDate

/**
 * Objeto utilitario para la lógica de ordenamiento de temas.
 * Extraído como función pura para facilitar los tests unitarios.
 */
object ThemeOrderHelper {

    // Mapeo de días de la semana a nombres de temas
    private val dayToThemeMap = mapOf(
        MONDAY to "Feliz Lunes",
        TUESDAY to "Feliz Martes",
        WEDNESDAY to "Feliz Miércoles",
        THURSDAY to "Feliz Jueves",
        FRIDAY to "Feliz Viernes",
        SATURDAY to "Feliz Fin de Semana",
        SUNDAY to "Feliz Fin de Semana"
    )

    /**
     * Obtiene el nombre del tema correspondiente al día actual.
     */
    fun getThemeNameForDay(date: LocalDate = LocalDate.now()): String {
        //si date.dayOfWeek == MONDAY -> Retorna "Feliz Lunes"
        return dayToThemeMap[date.dayOfWeek] ?: "Buenos Días"
    }

    /**
     * Reordena la lista de temas poniendo el tema del día actual primero.
     */
    fun reorderThemesWithTodayFirst(
        themes: List<ThemeModel>,
        todayThemeName: String = getThemeNameForDay()
    ): List<ThemeModel> {

        // Buscar el tema de hoy
        val todayTheme = themes.find {
            // verificar para cada tematica comparar si la tematica correspondiente
            // al dia de hoy se encuentra presente en la lista
            // Si todayThemeName = "Feliz Lunes"
            // buscara dentro de themes alguna tematica que coincida con todayThemeName
                theme ->
            theme.themeName.equals(todayThemeName, ignoreCase = true)
        }
        return if (todayTheme != null) {
            // Crear nueva lista con el de hoy primero
            listOf(todayTheme) +  //listOf(todayTheme) es una lista que contiene un unico elemento, el tema de hoy
                    themes.filter { // filter es otra función de orden superior que crea una nueva lista conteniendo solo los elementos que cumplen cierta condición. La condición aquí es que el ID del tema sea diferente del ID del tema de hoy.
                        // En otras palabras, estamos creando una lista con todos los temas excepto el de hoy.
                            theme ->
                        theme.themeId != todayTheme.themeId
                    }
        } else {
            // Si no se encuentra, devolver orden original
            themes
        }
    }
}