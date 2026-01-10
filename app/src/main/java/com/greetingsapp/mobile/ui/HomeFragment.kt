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
import com.greetingsapp.mobile.R
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
        currentTrackingCategory = getString(R.string.dia_buenos_dias)

        // 1. Mostrar shimmer al iniciar la carga del Home
        showLoading()

        //se lanzan estas dos funciones en un viewLifecycleOwner atado a la vista, no al fragment, ni a activities
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // ⭐ TRUCO: Retraso de 3 segundos (3000ms)
//                 Esto simula una conexión lenta a internet
//                kotlinx.coroutines.delay(3000)

                // ⭐ NUEVO: Llamada al endpoint inteligente
                val response = RetrofitClient.instance.getHomeContent()

                if (navId != currentNavigationId) {
                    Log.d("HomeFragment", "Navegación obsoleta, descartando")
                    return@launch
                }

                if (response.isSuccessful) {
                    val homeContent = response.body()

                    if (homeContent == null || homeContent.images.isEmpty()) {
                        showError(
                            message = "No hay contenido disponible",
                            canRetry = true,
                            onRetry = { loadHomeContent() }
                        )
                        return@launch

//                        Log.d(
//                            "HomeFragment", """
//                            🔍 DEBUG COMPLETO:
//                             - JSON recibido: ${response.body()}
//                             - contentType: '${homeContent.contentType}'
//                             - title: '${homeContent.title}'
//                             - images: ${homeContent.images.size}
//                             - Match SPECIAL_EVENT: ${homeContent.contentType == "SPECIAL_EVENT"}
//                            """.trimIndent()
//                        )
                    }

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
                    hideError()
                } else {
                    showError(
                        message = "Error al cargar inicio",
                        canRetry = true,
                        onRetry = { loadHomeContent() }
                    )
                }
            } catch (e: java.io.IOException) {
                if (navId != currentNavigationId) return@launch
                showError(
                    message = "Sin conexión a internet",
                    canRetry = true,
                    onRetry = { loadHomeContent() }
                )
            } catch (e: Exception) {
                if (navId != currentNavigationId) return@launch
                Log.e("HomeFragment", "Error: ${e.message}", e)
                showError(
                    message = "Error inesperado",
                    canRetry = true,
                    onRetry = { loadHomeContent() }
                )
            } finally {
                hideLoading()
            }
        }
    }

    // metodo encargado de mostrar los chips de tematicas y las imagenes del mismo
    private fun executeLoadThemes(categoryId: Long, categoryName: String) {

        // ⭐ AGREGAR ESTOS LOGS
//        Log.d(
//            "SHIMMER_DEBUG", """
//        🔍 Estado ANTES de cargar:
//        - shimmerTheme visibility: ${binding.shimmerContainerTheme.visibility}
//        - shimmerImg visibility: ${binding.shimmerContainerImg.visibility}
//        - recyclerThemes visibility: ${binding.recyclerViewThemes.visibility}
//        - recyclerImages visibility: ${binding.recyclerViewImages.visibility}
//    """.trimIndent()
//        )

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

        showLoading()
        viewLifecycleOwner.lifecycleScope.launch {
            try {

                // ⭐ TRUCO: Retraso de 3 segundos (3000ms)
                // Esto simula una conexión lenta a internet
//                kotlinx.coroutines.delay(2000)

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

                    // ✅ Solo verificar si la respuesta es null
                    if (themes.isEmpty()) {
                        Log.w("HomeFragment", "La categoría $categoryName no tiene temas")
                        // Opcionalmente mostrar mensaje informativo
                        Toast.makeText(
                            requireContext(),
                            "Esta categoría aún no tiene contenido",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@launch
                    }

                    // ⭐ Si es "Saludos", reordenamos la lista en el frontend
                    val orderedThemes =
                        if (categoryName.equals(currentTrackingCategory, ignoreCase = true)) {
                            reorderThemesWithTodayFirst(themes)
                        } else {
                            themes
                        }

                    // ⭐ Actualizar UI con éxito
                    updateUIWithSuccess(orderedThemes)

                    if (orderedThemes.isNotEmpty()) {
                        // ⭐ LÓGICA INTELIGENTE AQUÍ
                        var firstTheme = orderedThemes[0] // Por defecto el primero

                        // 1. Cargamos las imágenes de ese tema
                        loadImages(
                            firstTheme.themeId,
                            navId
                        ) //pasamos navId al metodo encargado de cargar las imagenes

                        // 2. Marcamos el chip visualmente
                        themesAdapter.selectThemeById(firstTheme.themeId)

                        // 3. Actualizamos el tracking
                        currentTrackingCategory = firstTheme.themeName
                    }
                } else {
                    // ❌ Error del servidor (404, 500, etc.)
                    showError(
                        message = "Error al cargar categorías (${response.code()})",
                        canRetry = true,
                        onRetry = { executeLoadThemes(categoryId, categoryName) }
                    )
                }
            } catch (e: java.io.IOException) {
                // ❌ Sin conexión a internet
                if (navId != currentNavigationId) return@launch

                showError(
                    message = "Sin conexión a internet.\nVerifica tu conexión e intenta nuevamente.",
                    canRetry = true,
                    onRetry = { executeLoadThemes(categoryId, categoryName) }
                )
            } catch (e: Exception) {
                // ❌ Otros errores inesperados
                if (navId != currentNavigationId) return@launch

                Log.e("HomeFragment", "Error inesperado: ${e.message}", e)
                showError(
                    message = "Ocurrió un error inesperado",
                    canRetry = true,
                    onRetry = { executeLoadThemes(categoryId, categoryName) }
                )
            } finally {
                //ocultar loading al terminar
                hideLoading()
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

                    // si la lista de imagenes de una tematica esta vacia
                    if (imagesList.isEmpty()) {
                        Log.w("HomeFragment", "El tema $themeId no tiene imágenes")

                        // Mostrar estado vacío específico para imágenes
                        showEmptyImagesState()
                    }else{
                        // ✅ Hay imágenes, mostrar normalmente
                        hideEmptyState()
                        imagesAdapter.submitList(imagesList)
                        _binding?.recyclerViewImages?.visibility = View.VISIBLE
                        Log.d("APP", "Cargadas ${imagesList.size} imágenes")
                    }
                    Log.d("APP", "Cargadas ${imagesList.size} imágenes")
                } else {
                    Log.e("API_ERROR", "Error del servidor: ${responseApi.code()}")
                    showError(
                        message = "Error al cargar imágenes",
                        canRetry = true,
                        onRetry = { loadImages(themeId, navId) }
                    )
                }
            } catch (e: java.io.IOException) {
                Log.e("API_ERROR", "Sin conexión: ${e.message}")
                showError(
                    message = "Sin conexión a internet",
                    canRetry = true,
                    onRetry = { loadImages(themeId, navId) }
                )
            } catch (e: Exception) {
                Log.e("API_ERROR", "Error inesperado: ${e.message}", e)
                showError(
                    message = "Error al cargar imágenes",
                    canRetry = true,
                    onRetry = { loadImages(themeId, navId) }
                )
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
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Opción A: Tienes endpoint (Ideal)
                val response = RetrofitClient.instance.searchImages(query)

                Log.d("HomeFragment", "Buscando: $query en el servidor...")

                if (navId != currentNavigationId) {
                    return@launch
                }

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
                Toast.makeText(requireContext(), "Error al buscar", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ⭐ Función auxiliar para obtener el nombre esperado (Pon esto al final de la clase)
    private fun getThemeNameForToday(): String {
        val today = LocalDate.now().dayOfWeek
        return when (today) {
            MONDAY -> getString(R.string.dia_lunes)
            TUESDAY -> getString(R.string.dia_martes)
            WEDNESDAY -> getString(R.string.dia_miercoles)
            THURSDAY -> getString(R.string.dia_jueves)
            FRIDAY -> getString(R.string.dia_viernes)
            SATURDAY, SUNDAY -> getString(R.string.dia_finde)
            else -> getString(R.string.dia_buenos_dias)
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

    //funciones para controlar la animacion

    // Mostrar animacion
    private fun showLoading() {
        binding.shimmerContainerImg.visibility = View.VISIBLE
        binding.shimmerContainerTheme.visibility = View.VISIBLE
        // AGREGAR ESTA LÍNEA: Reinicia la animación explícitamente
        binding.shimmerContainerImg.startShimmer()
        binding.shimmerContainerTheme.startShimmer()
    }

    // Ocultar animacion
    private fun hideLoading() {
        binding.shimmerContainerImg.stopShimmer()
        binding.shimmerContainerImg.visibility = View.GONE
        binding.shimmerContainerTheme.stopShimmer()
        binding.shimmerContainerTheme.visibility = View.GONE

        binding.recyclerViewImages.visibility = View.VISIBLE
    }

    // ========== FUNCIONES DE MANEJO DE ESTADOS ==========

    /**
     * Muestra una pantalla de error con mensaje personalizado
     * @param message Mensaje a mostrar al usuario
     * @param canRetry Si es true, muestra botón de reintentar
     * @param onRetry Acción a ejecutar cuando el usuario presiona reintentar
     */
    private fun showError(
        message: String,
        canRetry: Boolean = true,
        onRetry: (() -> Unit)? = null
    ) {
        _binding?.let { binding ->
            // Ocultar todo lo demás
            hideLoading()
            binding.recyclerViewImages.visibility = View.GONE
            binding.recyclerViewThemes.visibility = View.GONE
            binding.specialDayBanner.visibility = View.GONE

            // Mostrar vista de error
            binding.errorState.errorStateContainer.visibility = View.VISIBLE
            binding.errorState.errorMessage.text = message

            // Configurar botón de reintentar
            if (canRetry && onRetry != null) {
                binding.errorState.retryButton.visibility = View.VISIBLE
                binding.errorState.retryButton.setOnClickListener {
                    binding.errorState.errorStateContainer.visibility = View.GONE
                    onRetry()
                }
            } else {
                binding.errorState.retryButton.visibility = View.GONE
            }
        }
    }

    /**
     * Oculta la vista de error
     */
    private fun hideError() {
        _binding?.errorState?.errorStateContainer?.visibility = View.GONE
    }

    /**
     * Actualiza la UI cuando la carga es exitosa
     */
    private fun updateUIWithSuccess(themes: List<ThemeModel>) {
        hideError()
        hideLoading()

        _binding?.let { binding ->
            binding.recyclerViewThemes.visibility = View.VISIBLE
            binding.recyclerViewImages.visibility = View.VISIBLE
        }

        themesAdapter.submitList(themes)
    }

    /**
     * Muestra un mensaje cuando un tema no tiene imágenes
     */
    private fun showEmptyImagesState() {
        _binding?.let { binding ->
            binding.recyclerViewImages.visibility = View.GONE

            // Opción A: Mostrar la vista de error con mensaje personalizado
            binding.emptyState.emptyStateContainer.visibility = View.VISIBLE
        }
    }

    /**
     * Oculta el estado vacío
     */
    private fun hideEmptyState() {
        _binding?.emptyState?.emptyStateContainer?.visibility = View.GONE
    }

    // este metedo se ejecuta al cambiar/navegar/abandonar una vista
    override fun onDestroyView() {
        super.onDestroyView()
        _binding =
            null //anular todos los componentes inflados(objetos koltin) para liberar espacio
    }
}