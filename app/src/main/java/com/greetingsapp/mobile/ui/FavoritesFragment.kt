package com.greetingsapp.mobile.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.greetingsapp.mobile.data.local.AppDatabase
import com.greetingsapp.mobile.data.model.ImageModel
import com.greetingsapp.mobile.databinding.FragmentFavoritesBinding
import com.greetingsapp.mobile.ui.adapter.ImagesAdapter
import com.greetingsapp.mobile.ui.utils.calculateDynamicSpanCount
import com.greetingsapp.mobile.ui.viewmodel.FavoritesViewModel
import com.greetingsapp.mobile.ui.viewmodel.FavoritesViewModelFactory
import kotlinx.coroutines.launch

class FavoritesFragment : Fragment() {

    // _binding: Referencia a los botones y textos del XML.
    // Es nullable (?) porque cuando te vas de esta pantalla, Android borra la vista
    // para liberar RAM, así que esta variable debe pasar a ser 'null'.
    private var _binding: FragmentFavoritesBinding? = null
    // binding: Un "atajo seguro" (!!) para no escribir ? en todos lados.
    // Solo se puede usar mientras la vista esté viva.
    private val binding get() = _binding!!

    // ViewModel específico para esta pantalla
    private lateinit var viewModel: FavoritesViewModel

    // Reusamos el adaptador de imágenes que ya tienes (¡Reciclaje de código!)
    private lateinit var adapter: ImagesAdapter

    // --- CICLO DE VIDA: PASO 1 (Nacimiento de la Interfaz) ---
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // Aquí convertimos el archivo XML (texto) en objetos Java/Kotlin (memoria).
        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        return binding.root
    }

    // --- CICLO DE VIDA: PASO 2 (La Interfaz ya existe) ---
    // Este es el lugar SEGURO para configurar botones y listas.
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Conectar con el cerebro (ViewModel)
        setupViewModel()
        // 2. Preparar el Recycler (la lista visual)
        setupRecyclerView()
        // 3. Empezar a escuchar los datos
        observeFavorites()
    }

    private fun setupViewModel() {
        // ViewModelProvider es el "Gerente de ViewModels".
        // Le dices: "Dame el ViewModel de Favoritos".
        // El Gerente revisa: "¿Ya existe uno en memoria? Te lo doy. ¿No existe? Lo creo usando la Factory".
        val factory = FavoritesViewModelFactory(AppDatabase.getDatabase(requireContext()).favoriteDao())
        viewModel = ViewModelProvider(this, factory)[FavoritesViewModel::class.java]
    }

    private fun setupRecyclerView() {
        // Configurar el click: Al tocar un favorito, vamos al detalle igual que siempre
        adapter = ImagesAdapter { selectedImage ->

            val intent = Intent(requireContext(), ImageDetailActivity::class.java)

            intent.putExtra("EXTRA_IMAGE_URL", selectedImage.imageUrl)

            // Como ya es favorito, la categoría importa menos, pero podemos pasar algo genérico
            intent.putExtra("EXTRA_CATEGORY_NAME", "Mis Favoritos")

            startActivity(intent)
        }

        binding.rvFavorites.apply {
            // ⭐ AQUÍ ESTÁ LA MAGIA RESPONSIVE:
            val dynamicSpanCount = requireContext().calculateDynamicSpanCount(160)

            layoutManager = GridLayoutManager(requireContext(), dynamicSpanCount)
            adapter = this@FavoritesFragment.adapter // @FavoritesFragment especifica que se usara el adapter asociado al fragment(rvFavorites)
        }
    }

    // --- COMUNICACIÓN: FRAGMENT <-> VIEWMODEL ---
    private fun observeFavorites() {
        // viewLifecycleOwner: ¡CRUCIAL!
        // No usamos "this" (el Fragmento), usamos "la vista del Fragmento".
        // Si el usuario cambia de pestaña, la vista muere.
        // Al usar viewLifecycleOwner, esta corrutina se cancela automáticamente
        // cuando la vista muere, evitando crashes.
        viewLifecycleOwner.lifecycleScope.launch {
            // .collect: Aquí nos "suscribimos" a la tubería del ViewModel.
            // Cada vez que la base de datos cambia, el código de aquí adentro se ejecuta solo.
            viewModel.favoritesList.collect { entities ->

                // LÓGICA DE UI: Mostrar mensaje "Vacío" si no hay fotos
                if (entities.isEmpty()) {
                    binding.tvEmpty.visibility = View.VISIBLE
                    binding.rvFavorites.visibility = View.GONE
                } else {
                    binding.tvEmpty.visibility = View.GONE
                    binding.rvFavorites.visibility = View.VISIBLE

                    // TRUCO: Convertir de 'FavoriteEntity' (BD) a 'ImageModel' (Adapter)
                    val imageModels = entities.map { entity ->
                        ImageModel(
                            imageId = entity.id.toLong(),
                            // 1. Usamos la categoría como nombre (ya que no guardamos el nombre real)
                            imageName = entity.categoryTitle,
                            // 2. Pasamos un texto vacío o genérico porque este dato se perdió
                            imageDescription = "",
                            imageUrl = entity.imageUrl
                        )
                    }
                    adapter.submitList(imageModels)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}