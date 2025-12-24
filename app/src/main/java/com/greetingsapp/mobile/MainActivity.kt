package com.greetingsapp.mobile

import android.content.Intent
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
import kotlinx.coroutines.launch

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

        //  Cargar datos iniciales
        // Por defecto, cargamos la categoría 1 ("Buenos Días") al iniciar
        // loadThemes(categoryId = 2)

        // 3. Configurar los botones
        setupNavigation()

        // 4. Carga inicial:
        // Truco: Simular un clic automático en "Buenos Días" al abrir la app
        // Esto cargará los temas de la categoría 1 automáticamente
        binding.bottomNavigation.selectedItemId = R.id.nav_buenos_dias

    }

    private fun setupRecyclerViews() {
        // --- Configuración de Temáticas (Lista Horizontal) ---
        // Le pasamos la función lambda: qué hacer cuando tocan un tema
        themesAdapter = ThemesAdapter { selectedTheme ->
            // Acción: Mostrar mensaje y cargar imágenes de ese tema
            // oast.makeText(this, "Cargando: ${selectedTheme.themeName}", Toast.LENGTH_SHORT).show()
            loadImages(selectedTheme.themeId)
        }

        binding.recyclerViewThemes.apply { // apply es una scope function, Aplica las siguientes configuraciones a este objeto
            layoutManager =
                LinearLayoutManager(this@MainActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = themesAdapter
        }

        // --- Configuración de Imágenes (Grilla Vertical) ---
        imagesAdapter = ImagesAdapter {
            selectedImage -> //lo que ejecuta la lambda

                // Esta es la acción común para TODAS las secciones
                val intent = Intent(this, ImageDetailActivity::class.java)

                // Pasamos la URL de la imagen a la otra pantalla
                intent.putExtra("EXTRA_IMAGE_URL", selectedImage.imageUrl)

                startActivity(intent)
        }

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
                    Toast.makeText(this@MainActivity, "Error al cargar temas", Toast.LENGTH_SHORT)
                        .show()
                }
            } catch (e: Exception) {
                // Capturamos errores de red (sin internet, servidor caído)
                Log.e("API_ERROR", "Fallo: ${e.message}")
                Toast.makeText(this@MainActivity, "Sin conexión", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadImages(themeId: Long) {
        lifecycleScope.launch {
            try {
                val responseApi = RetrofitClient.instance.getImagesByTheme(themeId)

                if (responseApi.isSuccessful) {
                    // 1. Se obtiene el objeto "Page" completo
                    val pageResponse = responseApi.body()

                    // 2. Se saca la lista que está ADENTRO de la página (en el campo 'content')
                    // El operador ?. sirve por si pageResponse es nulo
                    // El operador ?: sirve para usar una lista vacía si todo falla
                    val imagesList = pageResponse?.content ?: emptyList()

                    // 3. Se la entregas al adaptador
                    imagesAdapter.submitList(imagesList)

                    Log.d("APP", "Cargadas ${imagesList.size} imágenes")
                } else {
                    Log.e("API_ERROR", "Error del servidor: ${responseApi.code()}")
                }
            } catch (e: Exception) {
                Log.e("API_ERROR", "Fallo al conectar: ${e.message}")
            }
        }
    }

    private fun setupNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            // cuando se selecciona un item de nuestro btmNavigation, se comparan usando el when, y en funcion del item
            // identificado se ejecuta X o Y bloque de codigo
            when (item.itemId) { //id del item seleccionado
                R.id.nav_inicio -> { //item.itemId == id.nav_inicio
                    // Limpiamos visualmente antes de cargar
                    themesAdapter.submitList(emptyList())
                    imagesAdapter.submitList(emptyList())
                    loadHomeContent() // <--- ¡AQUÍ ESTÁ LA MAGIA!
                    true
                }

                R.id.nav_buenos_dias -> {
                    // ESTE ES EL IMPORTANTE AHORA
                    // Al tocar el sol, cargamos la Categoría 1 (Buenos Días)
                    // y limpiamos las imágenes viejas para que se vea el cambio
                    imagesAdapter.submitList(emptyList())
                    loadThemes(categoryId = 1)
                    true
                }

                R.id.nav_festividades -> {
                    // Asumiremos que la Categoría 3 es "Festividades" en tu BD
                    imagesAdapter.submitList(emptyList())
                    loadThemes(categoryId = 3)
                    true
                }

                else -> false
            }
        }
    }

    // Metodo que carga la configuración de la pantalla "Inicio"
    private fun loadHomeContent() {
        // 1. Cargar Chips: Esta vez pedimos las Categorías Generales
        loadCategoriesAsChips()

        // 2. Cargar Imágenes: Pedimos las "Novedades"
        loadRecentImages()
    }

    // metodo que carga las cagetorias generales en los chips de la pantalla de inicio
    private fun loadCategoriesAsChips() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getCategories()
                if (response.isSuccessful) {
                    val categories = response.body() ?: emptyList()

                    // TRUCO: Convertimos las Categorías a ThemeModel visualmente
                    // para poder reusar el mismo ThemesAdapter sin crear uno nuevo.
                    // (Mapeamos id->id, name->name)
                    val fakeThemes = categories.map { category ->
                        ThemeModel(
                            themeId = category.categoryId,
                            themeName = category.categoryName
                        )
                    }
                    themesAdapter.submitList(fakeThemes)
                }
            } catch (e: Exception) {
                Log.e("API", "Error cargando categorías: ${e.message}")
            }
        }
    }

    // metodo que muestra las ultimas imagenes cargadas a la api
    private fun loadRecentImages() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getAllImages() // El método nuevo
                if (response.isSuccessful) {
                    val images = response.body()?.content ?: emptyList()
                    imagesAdapter.submitList(images)
                }
            } catch (e: Exception) {
                Log.e("API", "Error cargando novedades: ${e.message}")
            }
        }
    }
}