package com.greetingsapp.mobile.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.gms.ads.AdRequest
import com.greetingsapp.mobile.ads.AdManager
import com.greetingsapp.mobile.data.network.RetrofitClient
import com.greetingsapp.mobile.databinding.FragmentSearchBinding
import com.greetingsapp.mobile.ui.adapter.ImagesAdapter
import com.greetingsapp.mobile.ui.utils.calculateDynamicSpanCount
import com.greetingsapp.mobile.ui.utils.hideKeyboard
import com.greetingsapp.mobile.ui.utils.showKeyboard
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

class SearchFragment : Fragment() {

    // ➕ Número de clics antes de mostrar un intersticial
    companion object {
        private const val CLICKS_BEFORE_INTERSTITIAL = 3
    }

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private lateinit var imagesAdapter: ImagesAdapter

    // ➕ Contador de clics en imágenes (para intersticiales)
    private var imageClickCount = 0

    // Control de concurrencia, para prevenir race conditions
    private val navigationId = AtomicLong(0)
    private var currentNavigationId: Long = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    // 1. Configuración inicial al crear la vista
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearchLogic()       // Configura qué pasa al escribir o cerrar
        setupBackPressHandling() // Configura el botón físico "Atrás"
        openKeyboard()           // Fuerza la apertura del teclado al entrar
        // ➕ Cargar banner de AdMob
        loadBannerAd()
    }

    // ➕ Función para cargar el banner
    private fun loadBannerAd() {
        val adRequest = AdRequest.Builder().build()
        binding.adViewBanner.loadAd(adRequest)
    }

    // 2. Lógica del Buscador y detección de salida
    private fun setupSearchLogic() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrEmpty()) {
                    executeSearch(query)
                    // [UX] Al buscar, quitamos el foco para bajar el teclado y mostrar resultados limpios.
                    binding.searchView.clearFocus() // Quita el foco al buscar
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })
        // ⭐ SOLUCIÓN MAGISTRAL: Detectar la pérdida de foco
        // Cuando pulsas "Atrás" con el teclado abierto, el sistema cierra el teclado y quita el foco.
        // Aquí interceptamos ese momento exacto.
        binding.searchView.setOnQueryTextFocusChangeListener { _, hasFocus ->
            // Si perdió el foco (porque se cerró el teclado) Y el texto está vacío...
            if (!hasFocus && binding.searchView.query.isNullOrEmpty()) {
                // ... salimos inmediatamente.
                parentFragmentManager.popBackStack()
            }
        }

        // [LISTENER X] Maneja el botón visual "X" o cerrar del propio widget
        binding.searchView.setOnCloseListener {
            closeKeyboard()
            parentFragmentManager.popBackStack() // Salir directamente
            true // Retornar true indica que nosotros manejamos el evento
        }
    }

    // 3. Manejo manual del Botón Atrás (Hardware)
    private fun setupBackPressHandling() {
        //cuando se presiona el boton de volver atras
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // Verificamos si el campo de texto está vacío
                    if (binding.searchView.query.isNullOrEmpty()) {
                        // [OPTIMIZACIÓN] Desactivamos este callback para evitar bucles si se llama rápido.
                        isEnabled = false

                        // Paso A: Cerrar teclado visualmente
                        closeKeyboard()

                        // Paso B: Quitar foco lógico del SearchView.
                        // Esto es vital para que Android entienda que ya no estamos editando(escribiendo).
                        binding.searchView.clearFocus()

                        // Paso C: Salir de la pantalla (regresar al Home)
                        parentFragmentManager.popBackStack()
                    } else {
                        binding.searchView.setQuery("", false)
                    }
                }
            }
        )
    }

    // 4. Apertura forzada del teclado (Delay necesario)
    private fun openKeyboard() {
        // [COMPATIBILIDAD] Usamos postDelayed porque si intentamos abrir el teclado
        // inmediatamente al crear la vista, a veces la animación de transición del Fragment
        // bloquea la solicitud del teclado. 200-300ms es el estándar seguro.
        binding.searchView.postDelayed({
            // _binding puede ser null si el usuario salió muy rápido, usamos ?.let por seguridad
            _binding?.let {
                it.searchView.isIconified = false // Expande la vista
                it.searchView.requestFocus()      // Pide el foco al sistema
                it.searchView.showKeyboard()      // Llama al InputMethodManager
            }
        }, 300)
    }

    private fun closeKeyboard() {
        binding.searchView.hideKeyboard()
    }

    private fun setupRecyclerView() {
        imagesAdapter = ImagesAdapter { selectedImage ->
            // ➕ Incrementar contador y verificar si mostrar intersticial
            imageClickCount++

            if (imageClickCount >= CLICKS_BEFORE_INTERSTITIAL) {
                // Resetear contador
                imageClickCount = 0

                // Mostrar intersticial y luego navegar
                activity?.let { activity ->
                    AdManager.showInterstitialIfReady(activity) {
                        // Este callback se ejecuta cuando el usuario cierra el anuncio
                        navigateToImageDetail(selectedImage.imageUrl)
                    }
                }
            } else {
                // Navegar directamente sin anuncio
                navigateToImageDetail(selectedImage.imageUrl)
            }
        }

        binding.recyclerViewResults.apply {
            // ⭐ Usamos tu nueva función dinámica
            val dynamicSpan = requireContext().calculateDynamicSpanCount(160)
            layoutManager = GridLayoutManager(requireContext(), dynamicSpan)
            adapter = imagesAdapter
        }
    }

    // ➕ Función auxiliar para navegar a ImageDetailActivity
    private fun navigateToImageDetail(imageUrl: String) {
        val intent = Intent(requireContext(), ImageDetailActivity::class.java)
        intent.putExtra("EXTRA_IMAGE_URL", imageUrl)
        intent.putExtra("EXTRA_CATEGORY_NAME", "Búsqueda")
        startActivity(intent)
    }

    //    Ejecuta la búsqueda contra la API
    private fun executeSearch(query: String) {
        // Generamos ticket nuevo para invalidar cargas anteriores
        val navId = navigationId.incrementAndGet()
        currentNavigationId = navId

        imagesAdapter.submitList(emptyList()) // Limpiar lista anterior
        showLoading()

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Opción A: Tienes endpoint (Ideal)
                val response = RetrofitClient.instance.searchImages(query)

                if (navId != currentNavigationId) return@launch

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
                        hideError()
                        hideEmptyState()
                    } else {
                        showEmptyState("No se encontraron resultados para '$query'")
                    }
                } else {
                    showError("Error al buscar") { executeSearch(query) }
                }
            } catch (e: java.io.IOException) {
                if (navId == currentNavigationId) {
                    showError("Error de conexión") { executeSearch(query) }
                }

            } catch (e: Exception) {
                if (navId == currentNavigationId) {
                    showError("Error en busqueda") { executeSearch(query) }
                }
            } finally {
                if (navId == currentNavigationId) {
                    hideLoading()
                }
            }
        }
    }

    private fun showLoading() {
        binding.shimmerContainerImg.visibility = View.VISIBLE
        binding.shimmerContainerImg.startShimmer()
        binding.recyclerViewResults.visibility = View.GONE
        binding.errorState.errorStateContainer.visibility = View.GONE
        binding.emptyState.emptyStateContainer.visibility = View.GONE
    }

    private fun hideLoading() {
        binding.shimmerContainerImg.stopShimmer()
        binding.shimmerContainerImg.visibility = View.GONE
        binding.recyclerViewResults.visibility = View.VISIBLE
    }

    private fun showError(message: String, onRetry: () -> Unit) {
        hideLoading()
        binding.recyclerViewResults.visibility = View.GONE
        binding.errorState.errorStateContainer.visibility = View.VISIBLE
        binding.errorState.errorMessage.text = message
        binding.errorState.retryButton.setOnClickListener { onRetry() }
    }

    private fun hideError() {
        binding.errorState.errorStateContainer.visibility = View.GONE
    }

    private fun showEmptyState(message: String) {
        hideLoading()
        binding.recyclerViewResults.visibility = View.GONE
        binding.emptyState.emptyStateContainer.visibility = View.VISIBLE
        binding.emptyState.emptyMessage.text = message
    }

    private fun hideEmptyState() {
        binding.emptyState.emptyStateContainer.visibility = View.GONE
    }

    override fun onDestroyView() {
        // Asegurar que el teclado se cierre al destruir la vista
        view?.hideKeyboard()
        super.onDestroyView()
        _binding = null
    }
}