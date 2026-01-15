// CategoryImagesFragment.kt
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.greetingsapp.mobile.R
import com.greetingsapp.mobile.data.model.ThemeModel
import com.greetingsapp.mobile.data.network.RetrofitClient
import com.greetingsapp.mobile.databinding.FragmentCategoryImagesBinding
import com.greetingsapp.mobile.ui.ImageDetailActivity
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

class CategoryImagesFragment : Fragment() {

    //singleton usado como puente para comunicar las instancias del fragment con otras clases
    companion object {
        private const val ARG_CATEGORY_ID = "category_id"
        private const val ARG_CATEGORY_NAME = "category_name"
        private const val ARG_SHOW_THEMES = "show_themes"

        // FACTORY METHODS
        /**
         * Crear instancia que SOLO muestra imágenes de la categoría
         */
        fun newInstance(categoryId: Long, categoryName: String): CategoryImagesFragment {
            return CategoryImagesFragment().apply {     // 1. Crea el fragmento vacío
                arguments = Bundle().apply {    // 2. Crea la "Caja Fuerte" (Bundle)
                    putLong(ARG_CATEGORY_ID, categoryId)    // 3. Guarda el ID
                    putString(ARG_CATEGORY_NAME, categoryName)   // 4. Guarda el Nombre
                    putBoolean(ARG_SHOW_THEMES, false)  // 5. Configura el modo
                }
            }
        }

        /**
         * Crear instancia que muestra CHIPS DE TEMAS + imágenes
         * (Para la navegación desde bottom nav "Buenos Días")
         */
        fun newInstanceWithThemes(categoryId: Long, categoryName: String): CategoryImagesFragment {
            return CategoryImagesFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_CATEGORY_ID, categoryId)
                    putString(ARG_CATEGORY_NAME, categoryName)
                    putBoolean(ARG_SHOW_THEMES, true)
                }
            }
        }
    }

    private var _binding: FragmentCategoryImagesBinding? = null
    private val binding get() = _binding!!

    private lateinit var imagesAdapter: ImagesAdapter
    private lateinit var themesAdapter: ThemesAdapter

    private var categoryId: Long = 0
    private var categoryName: String = ""
    private var showThemes: Boolean = false
    private var currentThemeId: Long? = null

    // variables usadas para descartar peticiones hechas en una vista/pantalla, cuando el usuario cambia a otra
    private val navigationId = AtomicLong(0)
    private var currentNavigationId: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // aqui se pasan los datos a las variables globales, enviados dentro de la instancia del fragment

        // si lo que se envio es nulo o cero, por defecto se usa el 0
        categoryId = arguments?.getLong(ARG_CATEGORY_ID) ?: 0

        categoryName = arguments?.getString(ARG_CATEGORY_NAME) ?: "" // lo mismo para el nombre

        // Si no encuentra nada, usa 'false' por defecto (?: false)
        showThemes = arguments?.getBoolean(ARG_SHOW_THEMES) ?: false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCategoryImagesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()

        if (showThemes) {
            // ☀️ MODO SALUDOS (Desde Bottom Nav)

            // Muestra el diseño complejo con chips y oculta el título estático
            binding.tvCategoryTitle.visibility = View.GONE
            loadThemesAndImages()
        } else {
            // 🏠 MODO DESDE INICIO (Cuando haces click en una categoría en Home)

            // Muestra el diseño simple y pone el título estático
            binding.tvCategoryTitle.apply {
                text = categoryName // Usamos el nombre que llegó en los argumentos
                visibility = View.VISIBLE // hacerlo visible
            }

            // Ocultar recyclerView de temas en este modo (esto estaba bien)
            binding.recyclerViewThemes.visibility = View.GONE
            loadAllCategoryImages()
        }
    }

    private fun setupRecyclerView() {
        themesAdapter = ThemesAdapter { selectedTheme ->
            currentThemeId = selectedTheme.themeId
            themesAdapter.selectThemeById(selectedTheme.themeId)
            // ⭐ Actualizar título con el nombre del tema seleccionado
            binding.tvCategoryTitle.text = selectedTheme.themeName
            loadImagesByTheme(selectedTheme.themeId)
        }

        binding.recyclerViewThemes.apply {
            layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = themesAdapter
        }

        imagesAdapter = ImagesAdapter { selectedImage ->
            val intent = Intent(requireContext(), ImageDetailActivity::class.java)
            intent.putExtra("EXTRA_IMAGE_URL", selectedImage.imageUrl)
            // ⭐ Pasar el título actual (puede ser categoría o tema)
            intent.putExtra("EXTRA_CATEGORY_NAME", binding.tvCategoryTitle.text.toString())
            startActivity(intent)
        }

        binding.recyclerViewImages.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = imagesAdapter
        }
    }

    private fun clearAdapters() {
        imagesAdapter.submitList(emptyList())
    }

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

    // ==========================================
    // 1. CARGA INICIAL (Temas + Imágenes) en la vista de "Saludos"
    // ==========================================
    private fun loadThemesAndImages() {

        // 1. Generar ticket único para este clic
        val navId = navigationId.incrementAndGet()
        currentNavigationId = navId

        clearAdapters()

        // TRUE: Porque estamos cargando todo desde cero (incluyendo la barra de temas)
        showLoading(isInitialLoad = true)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // 1. Cargar temas de la categoría
                val themesResponse = RetrofitClient.instance.getThemesByCategory(categoryId)

                // 2. VERIFICAR: ¿Sigo siendo la petición más reciente?
                if (navId != currentNavigationId) return@launch // Si no, me detengo

                if (themesResponse.isSuccessful) {
                    val themes = themesResponse.body() ?: emptyList()

                    if (themes.isEmpty()) {
                        showEmptyImagesState("No hay temas disponibles")
                        return@launch
                    }

                    // 2. Reordenar para poner el tema de hoy primero
                    val orderedThemes = reorderThemesWithTodayFirst(themes)

                    // 3. Mostrar chips de temas
                    themesAdapter.submitList(orderedThemes)
                    binding.recyclerViewThemes.visibility = View.VISIBLE

                    // 4. Cargar imágenes del PRIMER tema (tema de hoy)
                    val todayTheme = orderedThemes.first()
                    currentThemeId = todayTheme.themeId
                    themesAdapter.selectThemeById(todayTheme.themeId)

                    // ⭐ Actualizar título con el nombre del tema de hoy
                    binding.tvCategoryTitle.text = todayTheme.themeName

                    // Cargamos las imágenes del primer tema
                    loadImagesByTheme(todayTheme.themeId)

                } else {
                    showError("Error al cargar temas") { loadThemesAndImages() }
                }

            } catch (e: java.io.IOException) {
                if (navId == currentNavigationId) {
                    showError("Sin conexión a internet") { loadThemesAndImages() }
                }
            } catch (e: Exception) {
                if (navId == currentNavigationId) {

                    Log.e("CategoryImages", "Error: ${e.message}", e)
                    showError("Error inesperado") { loadThemesAndImages() }
                }
            }
            finally {
                if(navId == currentNavigationId){ //es la misma solicitud/peticion?
                    hideLoading()
                }
            }
        }
    }

    // ==========================================
    // 2. CAMBIO DE TEMA (Solo animar carga de Imágenes)
    // ==========================================
    //metodo encargado de mostrar las imagenes de una categoria/tematica especifica
    private fun loadImagesByTheme(themeId: Long) {

        // 1. Generar ticket único para este clic
        val navId = navigationId.incrementAndGet()
        currentNavigationId = navId

        clearAdapters()
        // FALSE: Porque los temas YA están visibles, no queremos que parpadeen
        showLoading(false)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getImagesByTheme(themeId)

                // 2. VERIFICAR: ¿Sigo siendo la petición más reciente?
                if (navId != currentNavigationId) return@launch // Si no, me detengo

                if (response.isSuccessful) {
                    val images = response.body()?.content ?: emptyList()

                    if (images.isEmpty()) {
                        showEmptyImagesState("No hay imágenes en este tema")
                    } else {
                        hideEmptyState()
                        hideError()
                        imagesAdapter.submitList(images)
                    }
                } else {
                    showError("Error al cargar imágenes") { loadImagesByTheme(themeId) }
                }
            } catch (e: java.io.IOException) {
                if (navId == currentNavigationId) {
                    showError("Sin conexión a internet") { loadImagesByTheme(themeId) }
                }
            } catch (e: Exception) {
                if (navId == currentNavigationId) {
                    Log.e("CategoryImages", "Error: ${e.message}", e)
                    showError("Error inesperado") { loadImagesByTheme(themeId) }
                }
            } finally { //siempre se ejecutara PASE LO QUE PASE
                // Verificar en el finally para quitar el loading
                if (navId == currentNavigationId) {
                    hideLoading()
                }
            }
        }
    }

    // ==========================================
    // 3. CARGA CATEGORÍA SIMPLE (Desde Home)
    // ==========================================
    private fun loadAllCategoryImages() {

         val navId = navigationId.incrementAndGet()
        currentNavigationId = navId

        clearAdapters()

        // FALSE: En este modo NO usamos la barra de temas, así que no necesitamos su shimmer
        // (Aunque da igual porque el RecyclerView de temas está GONE en este modo)
        showLoading(isInitialLoad = false)

        // ⭐ Ocultar recyclerView de temas en este modo
        binding.recyclerViewThemes.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getImagesByCategory(categoryId)

                if(navId != currentNavigationId){
                    return@launch
                }
                if (response.isSuccessful) {
                    val images = response.body()?.content ?: emptyList()

                    if (images.isEmpty()) {
                        showEmptyImagesState("No hay imágenes en esta categoría.")
                    } else {
                        hideEmptyState()
                        hideError()
                        imagesAdapter.submitList(images)
                    }
                } else {
                    showError("Error al cargar imágenes") { loadAllCategoryImages() }
                }
            } catch (e: java.io.IOException) {
                if(navId == currentNavigationId){ //seguimos en la misma seccion/vista?
                showError("Sin conexión a internet") { loadAllCategoryImages() }
                }
            } catch (e: Exception) {
                if(navId == currentNavigationId){ //seguimos en la misma seccion/vista?

                Log.e("CategoryImages", "Error: ${e.message}", e)
                showError("Error inesperado") { loadAllCategoryImages() }
                }
            } finally { //siempre se ejecutara
                if(navId == currentNavigationId){ //seguimos en la misma seccion/vista?
                hideLoading()
                }
            }
        }
    }

    // ==================================================================
    // METODO ENCARGADO DE REORDENAR LA LISTA DE TEMAS EN FUNCION DEL DIA
    // ==================================================================
    private fun reorderThemesWithTodayFirst(themes: List<ThemeModel>): List<ThemeModel> {
        val todayThemeName = getThemeNameForToday()

        val todayTheme = themes.find { theme ->
            theme.themeName.equals(todayThemeName, ignoreCase = true)
        }

        return if (todayTheme != null) {
            listOf(todayTheme) + themes.filter { theme ->
                theme.themeId != todayTheme.themeId
            }
        } else {
            themes
        }
    }

    // ==========================================
    // 4. SHOW LOADING INTELIGENTE
    // ==========================================
    /**
     * @param isInitialLoad
     * true = Oculta todo y muestra shimmer de TEMAS y de IMÁGENES (Carga total)
     * false = Mantiene los temas visibles y solo muestra shimmer de IMÁGENES (Cambio de pestaña)
     */
    private fun showLoading(isInitialLoad: Boolean) {
        binding?.let { binding ->
            // 1. Siempre ocultar imágenes y errores
            binding.recyclerViewImages.visibility = View.GONE
            binding.errorState.errorStateContainer.visibility = View.GONE
            binding.emptyState.emptyStateContainer.visibility = View.GONE

            // 2. Lógica del Shimmer de Temas
            if (isInitialLoad && showThemes) {
                // Si es carga inicial Y estamos en modo "con temas"
                binding.recyclerViewThemes.visibility = View.GONE // Ocultamos la lista real
                binding.shimmerContainerTheme.visibility = View.VISIBLE // Mostramos esqueleto
                binding.shimmerContainerTheme.startShimmer()
            } else {
                // Si solo estamos cambiando de imagen, DEJAMOS LA LISTA DE TEMAS QUIETA
                // No tocamos binding.recyclerViewThemes.visibility aquí
                binding.shimmerContainerTheme.stopShimmer()
                binding.shimmerContainerTheme.visibility = View.GONE
            }

            // 3. Siempre mostrar shimmer de imágenes
            binding.shimmerContainerImg.visibility = View.VISIBLE
            binding.shimmerContainerImg.startShimmer()
        }
    }

    // oculta los componentes graficos usados para la carga
    private fun hideLoading() {
        _binding?.let { binding ->
            // ⭐ Detener y ocultar TODOS los shimmers
            binding.shimmerContainerTheme.stopShimmer()
            binding.shimmerContainerTheme.visibility = View.GONE

            binding.shimmerContainerImg.stopShimmer()
            binding.shimmerContainerImg.visibility = View.GONE

            // ⭐ Mostrar RecyclerView
            binding.recyclerViewImages.visibility = View.VISIBLE
        }
    }

    //muestra la vista de error
    private fun showError(
        message: String,
        canRetry: Boolean = true,
        onRetry: (() -> Unit)? = null
    ) {
        _binding?.let { binding ->
            // ⭐ Ocultar todo lo demás
            hideLoading()
            binding.recyclerViewImages.visibility = View.GONE
            binding.emptyState.emptyStateContainer.visibility = View.GONE

            // ⭐ Mostrar vista de error
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

    private fun hideError() {
        _binding?.errorState?.errorStateContainer?.visibility = View.GONE
    }

    // muestra vista de cuando no hay contenido disponible ya sea en una categoria o tematica
    private fun showEmptyImagesState(message: String) {
        _binding?.let { binding ->
            // ⭐ Ocultar todo lo demas
            hideLoading()
            binding.recyclerViewImages.visibility = View.GONE
            binding.errorState.errorStateContainer.visibility = View.GONE

            // ⭐ Mostrar estado vacío
            binding.emptyState.emptyStateContainer.visibility = View.VISIBLE
            binding.emptyState.emptyMessage.text = message
        }
    }

    private fun hideEmptyState() {
        _binding?.emptyState?.emptyStateContainer?.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}