package com.greetingsapp.mobile

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.greetingsapp.mobile.adapter.ImagesAdapter
import com.greetingsapp.mobile.adapter.ThemesAdapter
import com.greetingsapp.mobile.databinding.ActivityMainBinding
import com.greetingsapp.mobile.model.ThemeModel
import com.greetingsapp.mobile.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // Declaramos nuestros adaptadores
    private lateinit var themesAdapter: ThemesAdapter
    private lateinit var imagesAdapter: ImagesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Configurar ViewBinding
        // Infla el layout y obtiene una instancia de ActivityMainBinding con todos los elementos del layout
        // en forma de atributos de clase
        binding = ActivityMainBinding.inflate(layoutInflater)
        // Establece la vista raíz del layout(principal) como el contenido de la Activity
        setContentView(binding.root)

        // 2. Configurar los RecyclerViews
        setupRecyclerViews()

        // 3. Cargar datos iniciales
        // Por defecto, cargamos la categoría 1 ("Buenos Días") al iniciar
        loadThemes(categoryId = 2)
    }

    private fun setupRecyclerViews() {
        // --- Configuración de Temáticas (Lista Horizontal) ---
        // Le pasamos la función lambda: qué hacer cuando tocan un tema
        themesAdapter = ThemesAdapter {
            selectedTheme ->
            // Acción: Mostrar mensaje y cargar imágenes de ese tema
            Toast.makeText(this, "Cargando: ${selectedTheme.themeName}", Toast.LENGTH_SHORT).show()
            loadImages(selectedTheme.themeId)
        }

        binding.recyclerViewThemes.apply { // apply es una scope function, Aplica las siguientes configuraciones a este objeto
            layoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = themesAdapter
        }

        // --- Configuración de Imágenes (Grilla Vertical) ---
        imagesAdapter = ImagesAdapter()
        binding.recyclerViewImages.apply {
            layoutManager = GridLayoutManager(this@MainActivity, 2) // 2 Columnas
            adapter = imagesAdapter
        }
    }

    private fun loadThemes(categoryId: Long) {
        // Lanzamos una corrutina vinculada al ciclo de vida de esta Activity
        lifecycleScope.launch { //debido a la version de Retrofit, no es necesario especificar en que hilo se lanzara la corrutina
            try {
                // La app se "pausa" aquí esperando la respuesta, PERO la pantalla sigue respondiendo (no se congela)
                val response = RetrofitClient.instance.getThemesByCategory(categoryId)

                if (response.isSuccessful) {
                    val themes = response.body() ?: emptyList()
                    themesAdapter.submitList(themes)

                    // Cargar imágenes del primer tema automáticamente
                    if (themes.isNotEmpty()) {
                        loadImages(themes[0].themeId)
                    }
                } else {
                    Log.e("API_ERROR", "Error: ${response.code()}")
                    Toast.makeText(this@MainActivity, "Error al cargar temas", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                // Capturamos errores de red (sin internet, servidor caído)
                Log.e("API_ERROR", "Fallo: ${e.message}")
                Toast.makeText(this@MainActivity, "Sin conexión", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadImages(themeId: Long) {
        // TODO: Implementar la carga de imágenes en el siguiente paso
        // Aquí llamaremos a RetrofitClient.instance.getImagesByTheme(...)
        Log.d("APP", "Debería cargar imágenes del tema $themeId")
    }
}