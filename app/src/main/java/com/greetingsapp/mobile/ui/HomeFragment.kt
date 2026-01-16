package com.greetingsapp.mobile.ui

import CategoryImagesFragment
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
import com.greetingsapp.mobile.databinding.FragmentCategoryImagesBinding
import com.greetingsapp.mobile.databinding.FragmentHomeBinding
import com.greetingsapp.mobile.ui.adapter.ImagesAdapter
import com.greetingsapp.mobile.ui.adapter.ThemesAdapter
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var themesAdapter: ThemesAdapter
    private lateinit var imagesAdapter: ImagesAdapter

    // ⭐ NUEVO: Contador para identificar cada navegación de forma única
    private val navigationId = AtomicLong(0)
    private var currentNavigationId: Long = 0

    // ⭐ Renombrado para mayor claridad
    private var homeContentTitle: String = "Inicio"

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

        //carga la vista inicial por defecto
        loadHomeContent()

    }

    //Se configuran los recyclerViews
    private fun setupRecyclerViews() {
        // Configurar adaptador de Temáticas/Categorías
        themesAdapter = ThemesAdapter { selectedCategory ->
            // ⭐ Navegar a CategoryImagesFragment
            val fragment = CategoryImagesFragment.newInstance(
                categoryId = selectedCategory.themeId, // Asegúrate de usar el ID correcto del DTO
                categoryName = selectedCategory.themeName
            )

            parentFragmentManager.beginTransaction()
                .replace(
                    R.id.fragmentContainer,
                    fragment
                ) // R.id.fragmentContainer es el ID de tu FrameLayout/FragmentContainerView en activity_main.xml
                .addToBackStack(null) // ⭐ Permite volver atrás
                .commit()
        }

        binding.recyclerViewThemes.apply {
            layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = themesAdapter
        }

        imagesAdapter = ImagesAdapter { selectedImage ->
            val intent = Intent(requireContext(), ImageDetailActivity::class.java)
            intent.putExtra("EXTRA_IMAGE_URL", selectedImage.imageUrl)
            intent.putExtra("EXTRA_CATEGORY_NAME", homeContentTitle)
            startActivity(intent)
        }

        binding.recyclerViewImages.apply {
            // ✨ Reutilizando la lógica para mantener la consistencia
            val dynamicSpanCount = requireContext().calculateDynamicSpanCount(160)

            layoutManager = GridLayoutManager(requireContext(), dynamicSpanCount)
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
        // 1. Mostrar shimmer al iniciar la carga del Home
        showLoading()

        //se lanzan estas dos funciones en un viewLifecycleOwner atado a la vista, no al fragment, ni a activities
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                Log.d("HomeFragment", "🚀 Iniciando carga de HomeContent...")
                // ⭐ TRUCO: Retraso de 3 segundos (3000ms)
                // Esto simula una conexión lenta a internet
                // kotlinx.coroutines.delay(3000)

                // ⭐ NUEVO: Llamada al endpoint inteligente
                val response = RetrofitClient.instance.getHomeContent()

                // ⭐ AGREGAR LOG DE LA RESPUESTA
                Log.d("HomeFragment", """
                📦 Respuesta recibida:
                - isSuccessful: ${response.isSuccessful}
                - code: ${response.code()}
                - body: ${response.body()}
                - errorBody: ${response.errorBody()?.string()}
            """.trimIndent())

                if (navId != currentNavigationId) {
                    Log.d("HomeFragment", "Navegación obsoleta, descartando")
                    return@launch
                }

                if (response.isSuccessful) {
                    val homeContent = response.body()

                    // ⭐ AGREGAR LOG DETALLADO
                    Log.d("HomeFragment", """
                    🔍 Contenido parseado:
                    - homeContent es null: ${homeContent == null}
                    - images: ${homeContent?.images?.size ?: 0}
                    - contentType: ${homeContent?.contentType}
                    - title: ${homeContent?.title}
                """.trimIndent())

                    if (homeContent == null || homeContent.images.isEmpty()) {
                        showError(message = "No hay contenido disponible", onRetry = { loadHomeContent() })
                        return@launch

                        Log.d(
                            "HomeFragment", """
                            🔍 DEBUG COMPLETO:
                             - JSON recibido: ${response.body()}
                             - contentType: '${homeContent?.contentType}'
                             - title: '${homeContent?.title}'
                             - images: ${homeContent?.images?.size}
                             - Match SPECIAL_EVENT: ${homeContent?.contentType == "SPECIAL_EVENT"}
                            """.trimIndent()
                        )
                    }
                    // ⭐ Guardar título para tracking
                    homeContentTitle = homeContent.title ?: "Inicio"

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
                            homeContentTitle = homeContent.title

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
                    // ⭐ AGREGAR LOG
                    Log.e("HomeFragment", "❌ Error HTTP: ${response.code()}")
                    showError(message = "Error al cargar inicio", onRetry = { loadHomeContent() })
                }
            } catch (e: java.io.IOException) {
                Log.e("HomeFragment", "❌ Sin conexión: ${e.message}", e)
                //valalidar si el usuario aun esta en la misma ventana, si esta se muestra el error, si no esta, no hacer nada
                if (navId == currentNavigationId) {
                    showError(message = "Sin conexión a internet", onRetry = { loadHomeContent() })
                }
            } catch (e: Exception) {
                Log.e("HomeFragment", "Error: ${e.message}", e)
                if (navId == currentNavigationId) {
                    showError(message = "Error inesperado", onRetry = { loadHomeContent() })
                }
            } finally {
                if (navId == currentNavigationId) {
                    hideLoading()
                }
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
                themesAdapter.submitList(fakeThemes) //estas son categorias disfrazadas de tematicas(themes)
            }
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error cargando categorías (no crítico): ${e.message}")
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

        clearAdapters()
        showLoading()
        // UI: Ocultamos banner y mostramos loader
        _binding?.let { binding ->
            binding.specialDayBanner.visibility = View.GONE
            binding.recyclerViewThemes.visibility =
                View.GONE // Ocultamos temas durante búsqueda para limpiar UI
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Opción A: Tienes endpoint (Ideal)
                val response = RetrofitClient.instance.searchImages(query)

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
                        homeContentTitle = "Búsqueda: $query" // ⭐ Actualizar tracking
                        hideError()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "No se encontraron resultados para '$query'",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: java.io.IOException) {
                if (navId == currentNavigationId) {
                    showError("Sin conexión a internet") { executeSearch(query) }
                }
            } catch (e: Exception) {
                if (navId == currentNavigationId) {
                    Log.e("HomeFragment", "Error en búsqueda: ${e.message}", e)
                    Toast.makeText(requireContext(), "Error al buscar", Toast.LENGTH_SHORT).show()
                }
            } finally {
                if (navId == currentNavigationId) {
                    hideLoading()
                }
            }
        }
    }

    //funciones para controlar la animacion

    // Mostrar animacion
    private fun showLoading() {
        //hacer visibles los shimmers para animarlos
        binding.shimmerContainerImg.visibility = View.VISIBLE
        binding.shimmerContainerTheme.visibility = View.VISIBLE

        // AGREGAR ESTA LÍNEA: inicia la animación explícitamente
        binding.shimmerContainerImg.startShimmer()
        binding.shimmerContainerTheme.startShimmer()

        //ocultar recyclerView y contenedor de errores
        binding.recyclerViewImages.visibility = View.GONE
        binding.errorState.errorStateContainer.visibility = View.GONE
        binding.emptyState.emptyStateContainer.visibility = View.GONE
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

    //onRetry:       Nombre del parámetro
    //(() -> Unit)    Tipo: función que no recibe parámetros y no retorna nada
    //?                Es opcional (puede ser null)
    private fun showError(
        message: String,
        onRetry: (() -> Unit)?
    ) {
        _binding?.let { binding ->
            // Ocultar t0do lo demas
            hideLoading()
            binding.recyclerViewImages.visibility = View.GONE
            binding.recyclerViewThemes.visibility = View.GONE

            // Mostrar vista de error
            binding.errorState.errorStateContainer.visibility = View.VISIBLE
            binding.errorState.errorMessage.text = message
            // Configurar botón de reintentar
            binding.errorState.retryButton.visibility = View.VISIBLE

            //cuando se presiona el boton de reintentar
            binding.errorState.retryButton.setOnClickListener {
                binding.errorState.errorStateContainer.visibility = View.GONE
                if (onRetry != null) {
                    onRetry()
                }
            }
        }
    }
    /**
     * Oculta la vista de error
     */
    private fun hideError() {
        _binding?.errorState?.errorStateContainer?.visibility = View.GONE
    }

    // este metedo se ejecuta al cambiar/navegar/abandonar una vista
    override fun onDestroyView() {
        super.onDestroyView()
        _binding =
            null //anular todos los componentes inflados(objetos koltin) para liberar espacio
    }
}