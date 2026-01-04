package com.greetingsapp.mobile.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.greetingsapp.mobile.data.model.ThemeModel
import com.greetingsapp.mobile.data.network.RetrofitClient
import com.greetingsapp.mobile.databinding.FragmentHomeBinding
import com.greetingsapp.mobile.ui.adapter.ImagesAdapter
import com.greetingsapp.mobile.ui.adapter.ThemesAdapter
import kotlinx.coroutines.launch
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicLong

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var themesAdapter: ThemesAdapter
    private lateinit var imagesAdapter: ImagesAdapter
    private var currentTrackingCategory: String = "Saludos"

    private var pendingCategoryId: Long? = null
    private var pendingCategoryName: String? = null

    // ⭐ NUEVO: Contador para identificar cada navegación de forma única
    private val navigationId = AtomicLong(0)
    private var currentNavigationId: Long = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // se configuran los recyclerViews
        setupRecyclerViews()

        // se configura la logica del buscador
        setupSearchLogic()

        //si el valor de pendingCategoryId no es nulo
        if (pendingCategoryId != null) {
            executeLoadThemes(pendingCategoryId!!, pendingCategoryName ?: "Categoría")

            //se reinician las variables en caso de cambiar de vista
            pendingCategoryId = null
            pendingCategoryName = null
        } else {

            //carga la vista inicial por defecto
            loadHomeContent()
        }
    }

    //Se configuran los recyclerViews
    private fun setupRecyclerViews() {
        themesAdapter = ThemesAdapter { selectedTheme ->
            currentTrackingCategory = selectedTheme.themeName
            // ⭐ Marcamos visualmente el que el usuario tocó
            themesAdapter.selectThemeById(selectedTheme.themeId)
            loadImages(selectedTheme.themeId)
        }

        binding.recyclerViewThemes.apply {
            layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = themesAdapter
        }

        imagesAdapter = ImagesAdapter { selectedImage ->
            val intent = Intent(requireContext(), ImageDetailActivity::class.java)
            intent.putExtra("EXTRA_IMAGE_URL", selectedImage.imageUrl)
            intent.putExtra("EXTRA_CATEGORY_NAME", currentTrackingCategory)
            startActivity(intent)
        }

        binding.recyclerViewImages.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = imagesAdapter
        }
    }

    private fun clearAdapters() {
        themesAdapter.submitList(emptyList())
        imagesAdapter.submitList(emptyList())
    }

    fun loadHomeContent() {
        // ⭐ Generar nuevo ID de navegación
        val navId = navigationId.incrementAndGet()
        currentNavigationId = navId

        clearAdapters()
        currentTrackingCategory = "Inicio"

        //se lanzan estas dos funciones en un viewLifecycleOwner atado a la vista, no al fragment, ni a activities
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // ⭐ NUEVO: Llamada al endpoint inteligente
                val response = RetrofitClient.instance.getHomeContent()

                if (navId != currentNavigationId) {
                    Log.d("HomeFragment", "Navegación obsoleta, descartando")
                    return@launch
                }

                if (response.isSuccessful) {
                    val homeContent = response.body()

                    if (homeContent != null) {
                        Log.d(
                            "HomeFragment", """
                            🔍 DEBUG COMPLETO:
                             - JSON recibido: ${response.body()}
                             - contentType: '${homeContent.contentType}'
                             - title: '${homeContent.title}'
                             - images: ${homeContent.images.size}
                             - Match SPECIAL_EVENT: ${homeContent.contentType == "SPECIAL_EVENT"}
                            """.trimIndent()
                        )

                        // ⭐ Actualizar UI según el tipo de contenido
                        when (homeContent.contentType) {
                            "SPECIAL_EVENT" -> {
                                // 🎉 DÍA ESPECIAL
                                _binding?.let { binding ->  //?. = si el objeto _binding no es nulo
                                    // Mostrar banner
                                    binding.specialDayBanner.visibility = View.VISIBLE
                                    binding.tvSpecialDayTitle.text = homeContent.title

                                    //  MANTENER visible el SearchView para búsquedas
                                    binding.searchView.visibility = View.VISIBLE
                                    // MANTENER visible los chips de categorías para navegación
                                    binding.recyclerViewThemes.visibility = View.VISIBLE
                                }
                                // Cargar las categorías (el usuario puede cambiar si quiere)
                                loadCategoriesAsChips(navId)
                                // Mostrar imágenes del evento
                                imagesAdapter.submitList(homeContent.images)
                                currentTrackingCategory = homeContent.title

                                Log.d(
                                    "HomeFragment", """
                                    🎨 UI actualizada:
                                    - Banner visible: ${binding.specialDayBanner.visibility == View.VISIBLE}
                                    - SearchView visible: ${binding.searchView.visibility == View.VISIBLE}
                                    - Themes visible: ${binding.recyclerViewThemes.visibility == View.VISIBLE}
                                """.trimIndent()
                                )
                            }


                            else -> {
                                // 📅 DÍA NORMAL: Mostrar categorías + últimas imágenes
                                _binding?.let { binding ->
                                    // Ocultar banner
                                    binding.specialDayBanner.visibility = View.GONE

                                    // Mostrar SearchView y chips
                                    binding.searchView.visibility = View.VISIBLE
                                    binding.recyclerViewThemes.visibility = View.VISIBLE
                                }

                                // Cargar categorías como chips
                                loadCategoriesAsChips(navId)
                                // Mostrar últimas imágenes
                                imagesAdapter.submitList(homeContent.images)

                                Log.d(
                                    "HomeFragment",
                                    "📅 Contenido normal: ${homeContent.images.size} imágenes"
                                )
                            }
                        }
                    }
                } else {
                    Log.e("HomeFragment", "Error: ${response.code()}")
                    Toast.makeText(requireContext(), "Error al cargar inicio", Toast.LENGTH_SHORT)
                        .show()
                }
            } catch (e: Exception) {
                Log.e("HomeFragment", "Error al cargar inicio: ${e.message}")
                if (isAdded) {
                    Toast.makeText(requireContext(), "Sin conexión", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // metodo encargado de mostrar los chips de tematicas y las imagenes del mismo
    private fun executeLoadThemes(categoryId: Long, categoryName: String) {

        //para ocultar el searcView cuando se cambie de seccion
        _binding?.searchView?.visibility = View.GONE

        // 1. Generamos un "Ticket" para esta petición (ej: Ticket #1)
        val navId = navigationId.incrementAndGet()
        currentNavigationId = navId //igualamos al nuevo "Ticket" generado = 1

        clearAdapters()
        currentTrackingCategory = categoryName

        // ⭐ Ocultar banner de día especial cuando navegas a categorías
        _binding?.specialDayBanner?.visibility = View.GONE
        _binding?.recyclerViewThemes?.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                //llamada a la API
                val response = RetrofitClient.instance.getThemesByCategory(categoryId)

                // 2. VERIFICACIÓN DE SEGURIDAD
                // Cuando la API responde, preguntamos:
                // "¿Sigue siendo el Ticket #1 el actual?"
                // Si el usuario pulsó otro botón mientras esperábamos, currentNavigationId sería #2.
                if (navId != currentNavigationId) {
                    // Si los tickets no coinciden, es una respuesta vieja. LA DESCARTAMOS
                    Log.d(
                        "HomeFragment",
                        "Navegación obsoleta ($categoryName), descartando resultado"
                    )
                    return@launch
                }

                // Si navId == currentNavigation, si coinciden, aun estamos en la misma vista, por lo que actualizamos
                if (response.isSuccessful) {
                    val themes = response.body() ?: emptyList()

                    // ⭐ Si es "Saludos", reordenamos la lista en el frontend
                    val orderedThemes =
                        if (categoryName.equals(currentTrackingCategory, ignoreCase = true)) {
                            reorderThemesWithTodayFirst(themes)
                        } else {
                            themes
                        }
                    themesAdapter.submitList(orderedThemes)

                    if (orderedThemes.isNotEmpty()) {
                        // ⭐ LÓGICA INTELIGENTE AQUÍ
                        var firstTheme  = orderedThemes[0] // Por defecto el primero

                        // 1. Cargamos las imágenes de ese tema
                        loadImages(firstTheme.themeId, navId) //pasamos navId al metodo encargado de cargar las imagenes

                        // 2. Marcamos el chip visualmente
                        themesAdapter.selectThemeById(firstTheme.themeId)

                        // 3. Actualizamos el tracking
                        currentTrackingCategory = firstTheme.themeName
                    }
                }
            } catch (e: Exception) {
                Log.e("HomeFragment", "Error: ${e.message}")
                if (isAdded) { //nos aseguramos que el fragment aun este conectado a la activity
                    Toast.makeText(requireContext(), "Error al cargar", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // metodo encargado de cargar las imagenes
    private fun loadImages(themeId: Long, navId: Long = currentNavigationId) {
        // Cuando el usuario navega manualmente a un tema específico,
        // asumimos que ya no está interesado en el evento especial del día,
        // por lo que ocultamos el banner para darle más espacio visual
        _binding?.specialDayBanner?.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val responseApi = RetrofitClient.instance.getImagesByTheme(themeId)

                //  Solo actualizar si navId == currentNavigationId, quiere decir que
                //  esta navegación sigue siendo la actual
                if (navId != currentNavigationId) {
                    Log.d("HomeFragment", "Navegación obsoleta (imágenes), descartando resultado")
                    return@launch //para abandorar una corrutina -> launch
                }

                if (responseApi.isSuccessful) {
                    val pageResponse = responseApi.body()
                    val imagesList = pageResponse?.content ?: emptyList()
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

    // cargar los chips de todas las categorias
    private suspend fun loadCategoriesAsChips(navId: Long) {
        try {
            val response = RetrofitClient.instance.getCategories()

            // ⭐ Verificar antes de actualizar UI
            if (navId != currentNavigationId) {
                Log.d("HomeFragment", "Navegación obsoleta (categorías), descartando")
                return
            }

            if (response.isSuccessful) {
                val categories = response.body() ?: emptyList()
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

    // COMUNICACIÓN: ACTIVITY -> FRAGMENT
    // Este metodo es el "buzón" donde MainActivity deja los mensajes.
    fun setCategoryToLoad(categoryId: Long, categoryName: String) {
        // Lógica del -1 que arreglamos antes:
        if (categoryId == -1L) {
            pendingCategoryId = null // Borrar pendientes -> Ir a Inicio
        } else {
            pendingCategoryId = categoryId // Guardar pendiente -> Ir a Categoría
        }
    }

    // ⭐ NUEVO: Lógica completa del SearchView
    private fun setupSearchLogic() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

            // 1. Se ejecuta cuando el usuario presiona "Enter" o el icono de lupa en el teclado
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrEmpty()) {
                    executeSearch(query) //metodo encargado de hacer la busqueda a la API
                    binding.searchView.clearFocus() // Ocultar teclado
                }
                return true
            }

            // 2. Se ejecuta cada vez que el usuario escribe una letra
            override fun onQueryTextChange(newText: String?): Boolean {
                // si el usuario borra t0do el texto se recargara automaticamente el contenido de home
                if (newText.isNullOrEmpty()) {
                    // Si el usuario borra manualmente t0do, podemos recargar el home
                    // o simplemente esperar
                    loadHomeContent()
                }
                return false
            }
        })

        // 3. Detectar cuando cierran el buscador (la X)
        binding.searchView.setOnCloseListener {
            loadHomeContent() // Volver al estado inicial
            false
        }
    }

    // ⭐ NUEVO: Ejecuta la búsqueda contra la API
    private fun executeSearch(query: String) {
        // Generamos ticket nuevo para invalidar cargas anteriores
        val navId = navigationId.incrementAndGet()
        currentNavigationId = navId

        currentTrackingCategory = "Búsqueda: $query"
        clearAdapters()

        // UI: Ocultamos banner y mostramos loader si tuvieras uno (recomendado)
        _binding?.let { binding ->
            binding.specialDayBanner.visibility = View.GONE
            binding.recyclerViewThemes.visibility =
                View.GONE // Ocultamos temas durante búsqueda para limpiar UI
            binding.progressBar.visibility = View.VISIBLE // Asumiendo que tienes una ProgressBar
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Opción A: Tienes endpoint (Ideal)
                val response = RetrofitClient.instance.searchImages(query)

                Log.d("HomeFragment", "Buscando: $query en el servidor...")

                if (navId != currentNavigationId) {
                    return@launch
                }

                _binding?.progressBar?.visibility = View.GONE //ocultar

                // Si tuvieras respuesta real:
                if (response.isSuccessful) {
                    //obtener objeto "Page" completo
                    val pageResponse = response.body()

                    // 2. Se saca la lista que está ADENTRO de la página (en el campo 'content')
                    // El operador ?. sirve por si pageResponse es nulo
                    // El operador ?: sirve para usar una lista vacia si t0do falla
                    val imagesList = pageResponse?.content ?: emptyList()

                    if (imagesList.isNotEmpty()) {
                        imagesAdapter.submitList(imagesList)
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "No se encontraron resultados para '$query'",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                if (navId != currentNavigationId) return@launch
                Log.e("HomeFragment", "Error en búsqueda: ${e.message}")
                _binding?.progressBar?.visibility = View.GONE
                Toast.makeText(requireContext(), "Error al buscar", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ⭐ Función auxiliar para obtener el nombre esperado (Pon esto al final de la clase)
    private fun getThemeNameForToday(): String {
        val today = LocalDate.now().dayOfWeek
        return when (today) {
            MONDAY -> "Feliz Lunes"
            TUESDAY -> "Feliz Martes"
            WEDNESDAY -> "Feliz Miércoles"
            THURSDAY -> "Feliz Jueves"
            FRIDAY -> "Feliz Viernes"
            SATURDAY, SUNDAY -> "Feliz Fin de Semana"
            else -> "Buenos Días"
        }
    }

    // ⭐ Función auxiliar para reordenar
    private fun reorderThemesWithTodayFirst(themes: List<ThemeModel>): List<ThemeModel> {
        val todayThemeName = getThemeNameForToday()

        // Buscar el tema de hoy
        val todayTheme = themes.find {
            // verificar para cada tematica comparar si la tematica correspondiente
            // al dia de hoy se encuentra presente en la lista
            // Si todayThemeName = "Feliz Lunes"
            // buscara dentro de themes alguna tematica que coincida con todayThemeName
            theme -> theme.themeName.equals(todayThemeName, ignoreCase = true)
        }

        return if (todayTheme != null) {
            // Crear nueva lista con el de hoy primero
            listOf(todayTheme)+  //listOf(todayTheme) es una lista que contiene un unico elemento, el tema de hoy
                    themes.filter { // filter es otra función de orden superior que crea una nueva lista conteniendo solo los elementos que cumplen cierta condición. La condición aquí es que el ID del tema sea diferente del ID del tema de hoy.
                        // En otras palabras, estamos creando una lista con todos los temas excepto el de hoy.
                theme -> theme.themeId != todayTheme.themeId
               }
        } else {
            // Si no se encuentra, devolver orden original
            themes
        }
    }

    // este metedo se ejecuta al cambiar/navegar/abandonar una vista
    override fun onDestroyView() {
        super.onDestroyView()
        _binding =
            null //anular todos los componentes inflados(objetos koltin) para liberar espacio
    }}