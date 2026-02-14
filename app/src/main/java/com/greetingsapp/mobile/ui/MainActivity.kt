package com.greetingsapp.mobile.ui

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.greetingsapp.mobile.R
import com.greetingsapp.mobile.databinding.ActivityMainBinding

//Este es el cerebro de la navegacion
class MainActivity : AppCompatActivity() {

    //convertir los elementos visuales en objetos Kotlin/Java
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // savedInstanceState == null significa: "La app acaba de nacer desde cero".
        // Si NO es null, significa que giraste el celular y Android ya recreó
        // lo que había, así que no debemos cargar el fragmento inicial de nuevo (se duplicaría).
        if (savedInstanceState == null) {
            // ⭐ SOLUCIÓN: No llamar loadHomeContent() aquí
            // Dejar que el Fragment se cargue solo con su lógica en onViewCreated
            val initialFragment = createHomeFragment()
            loadFragment(initialFragment)

            binding.bottomNavigation.selectedItemId = R.id.nav_inicio
        }
        setupNavigation()
    }

    private fun createHomeFragment() = HomeFragment()
    private fun createFavoritesFragment() = FavoritesFragment()

    // metodo encargado de la navegacion entre fragments(vistas/pantallas)
    private fun setupNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            // COMUNICACIÓN: ACTIVITY -> FRAGMENT
            // La Activity no "habla" con el fragmento mientras está vivo.
            // La estrategia segura es:
            // 1. Crear una instancia NUEVA del fragmento.
            // 2. Configurar sus datos iniciales (setCategoryToLoad).
            // 3. Reemplazar el viejo con el nuevo
            when (item.itemId) {
                R.id.nav_inicio -> {
                    val homeFragment = createHomeFragment()
                    // ⭐ No configurar pending, dejar que cargue por defecto
                    loadFragment(homeFragment)
                    true
                }

                R.id.nav_buenos_dias -> {
                    Log.d("TRACKING", "Navegación: Buenos Días")

                    // ⭐ NUEVO: Navegar directamente a CategoryImagesFragment y cargar la instancia que contiene las tematicas
                    val categoryFragment = CategoryImagesFragment.newInstanceWithThemes(
                        categoryId = 1L, // ID de la categoría "Saludos/Buenos Días"
                        categoryName = getString(R.string.dia_buenos_dias)
                    )
                    loadFragment(categoryFragment)
                    true
                }

                R.id.nav_favoritos -> {
                    Log.d("TRACKING", "Navegación: Mis Favoritos")

                    val favoritesFragment = createFavoritesFragment()
                    loadFragment(favoritesFragment)
                    true
                }

                // En setupNavigation(), cambiar nav_festividades por nav_about:
                R.id.nav_about -> {
                    Log.d("TRACKING", "Navegación: Acerca de")
                    val aboutFragment = AboutFragment()
                    loadFragment(aboutFragment)
                    true
                }

                else -> false
            }
        }
    }

    //metodo encargado de cargar cada fragment
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            // .replace: "Saca lo que haya en 'fragmentContainer' y pon este nuevo 'fragment'".
            // Al hacer esto, el fragmento anterior pasa por onDestroyView y muere.
            .replace(R.id.fragmentContainer, fragment)
            .commit() // "¡Acción!"
    }
}