package com.greetingsapp.mobile.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
import java.util.concurrent.atomic.AtomicLong

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var themesAdapter: ThemesAdapter
    private lateinit var imagesAdapter: ImagesAdapter
    private var currentTrackingCategory: String = "Buenos dias"

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

        //asegurar que se vea el searhView
        _binding?.searchView?.visibility = View.VISIBLE

        //se lanzan estas dos funciones en un viewLifecycleOwner atado a la vista, no al fragment, ni a activities
        viewLifecycleOwner.lifecycleScope.launch {
            loadCategoriesAsChips(navId)
            loadRecentImages(navId)
        }
    }

    private fun executeLoadThemes(categoryId: Long, categoryName: String) {

        //para ocultar el searcView cuando se cambie de seccion
        _binding?.searchView?.visibility = View.GONE

        // 1. Generamos un "Ticket" para esta petición (ej: Ticket #1)
        val navId = navigationId.incrementAndGet()
        currentNavigationId = navId //igualamos al nuevo "Ticket" generado = 1

        clearAdapters()
        currentTrackingCategory = categoryName

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
                    themesAdapter.submitList(themes)

                    if (themes.isNotEmpty()) {
                        loadImages(
                            themes[0].themeId,
                            navId
                        ) //pasamos navId al metodo encargado de cargar las imagenes
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

    // metodo que carga las ultimas imagenes cargadas a la api
    private suspend fun loadRecentImages(navId: Long) {
        try {
            val response = RetrofitClient.instance.getAllImages()

            // ⭐ Verificar antes de actualizar UI
            if (navId != currentNavigationId) {
                Log.d("HomeFragment", "Navegación obsoleta (recientes), descartando")
                return //abandona la funcion directamente
            }

            if (response.isSuccessful) {
                val images = response.body()?.content ?: emptyList()
                imagesAdapter.submitList(images)
            }
        } catch (e: Exception) {
            Log.e("API", "Error cargando novedades: ${e.message}")
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

    // este metedo se ejecuta al cambiar/navegar/abandonar una vista
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null //anular todos los componentes inflados(objetos koltin) para liberar espacio
    }
}