package com.greetingsapp.mobile.ui

import android.content.ClipData
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import coil.load
import com.google.android.gms.ads.AdRequest
import com.greetingsapp.mobile.R
import com.greetingsapp.mobile.data.local.AppDatabase
import com.greetingsapp.mobile.databinding.ActivityImageDetailBinding
import com.greetingsapp.mobile.ui.viewmodel.ImageDetailViewModel
import com.greetingsapp.mobile.ui.viewmodel.ImageDetailViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

//Esta clase maneja los píxeles, los clicks y la navegación. Se conecta al ImageDetailViewModel para saber qué pintar.
class ImageDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImageDetailBinding

    // Referencia al ViewModel
    private lateinit var viewModel: ImageDetailViewModel

    // Variables para datos
    private var currentImageUrl: String? = null
    private var currentCategory: String? = null

    companion object {
        const val EXTRA_IMAGE_URL = "EXTRA_IMAGE_URL"
        const val EXTRA_CATEGORY_NAME =
            "EXTRA_CATEGORY_NAME" // Agregamos esta para guardar el título en BD
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflar la vista: Convierte los elementos del XML en objetos Java/Kotlin en memoria
        binding = ActivityImageDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Obtener datos del Intent
        currentImageUrl = intent.getStringExtra(EXTRA_IMAGE_URL)
        currentCategory = intent.getStringExtra(EXTRA_CATEGORY_NAME)

        // Validación básica
        if (currentImageUrl == null) {
            Toast.makeText(this, getString(R.string.msg_error_cargar_imagen), Toast.LENGTH_SHORT)
                .show()
            finish()
            return
        }

        // 2. Configurar ViewModel (Base de Datos)
        setupViewModel()

        // 3. Cargar Imagen
        setupImageLoading()

        // 4. Configurar Botones
        setupButtons()

        // 5. Le pedimos al ViewModel que verifique en BD si esta foto ya tiene like.
        viewModel.checkFavoriteStatus(currentImageUrl!!)

        // 6. ➕ Cargar banner de AdMob
        loadBannerAd()
    }

    // ➕ Función para cargar el banner
    private fun loadBannerAd() {
        val adRequest = AdRequest.Builder().build()
        binding.adViewBanner.loadAd(adRequest)
    }

    // metodo que se encarga de configurar el viewModel
    private fun setupViewModel() {
        // 1. Obtener la Base de Datos (Singleton).
        // Si no existe, crea el archivo .db. Si existe, devuelve la conexión abierta.
        val database = AppDatabase.getDatabase(applicationContext)
        // 2. Obtener el DAO.
        // El DAO es la interfaz que contiene los métodos SQL (insert, delete, query).
        val dao = database.favoriteDao()
        // 3. Crear la Fábrica.
        // Empaquetamos el DAO dentro de la fábrica para pasárselo al ViewModel.
        val factory = ImageDetailViewModelFactory(dao)

        // Obtener/Crear el ViewModel.
        // ViewModelProvider es un gestor del sistema Android.
        // Le dice: "Dame el ImageDetailViewModel asociado a ESTA Activity".
        // - Si es la primera vez: Usa la 'factory' para hacer un 'new ImageDetailViewModel(dao)'.
        // - Si rotaron la pantalla: Devuelve la misma instancia que ya existía en memoria (NO crea una nueva).
        viewModel = ViewModelProvider(this, factory)[ImageDetailViewModel::class.java]

        // lifecycleScope: Una corrutina atada a la vida visual de la Activity.
        // Si la activity se cierra, esto deja de ejecutarse para ahorrar batería.
        lifecycleScope.launch {

            // viewModel.isFavorite.collect:
            // Aquí nos "suscribimos" al StateFlow.
            // Esta línea de código se queda PAUSADA (suspendida) esperando señales.
            // CADA VEZ que el ViewModel haga "_isFavorite.value = algo",
            // este bloque de código se despierta y ejecuta lo de adentro.
            viewModel.isFavorite.collect { isFavorite ->
                // Recibimos el valor booleano (true/false) y actualizamos el icono.
                updateFavoriteIcon(isFavorite)
            }
        }
    }

    private fun setupImageLoading() {
        // Usamos la URL recibida
        binding.ivDetail.load(currentImageUrl) {
            crossfade(true) // Efecto visual suave
            allowHardware(false) //nos permite leer sus píxeles para luego hacer una copia y poder compartirla
            placeholder(android.R.drawable.ic_menu_gallery)
            error(android.R.drawable.stat_notify_error)

            // Listener para ocultar ProgressBar (asumiendo que está en el XML nuevo)
            listener(
                onStart = { binding.progressBar.visibility = android.view.View.VISIBLE },
                onSuccess = { _, _ -> binding.progressBar.visibility = android.view.View.GONE },
                onError = { _, _ -> binding.progressBar.visibility = android.view.View.GONE }
            )
        }
    }

    // metodo que se encarga de configurar los botones
    private fun setupButtons() {
        //BOTON DE COMPARTIR
        binding.btnShare.setOnClickListener {
            shareImageProcess()
        }

        //BOTON DE FAVORITOS
        binding.btnFavorite.setOnClickListener {
            // Cuando el usuario toca, NO cambiamos el icono aquí directamente.
            // Solo le "avisamos" al ViewModel: "Oye, procesa este cambio".
            // El ViewModel hará la lógica de BD y, al terminar, actualizará el StateFlow.
            // Eso disparará el 'collect' de arriba, y recién ahí cambiará el icono.
            // Esto asegura que el icono solo cambie si la lógica fue exitosa.
            viewModel.toggleFavorite(
                imageUrl = currentImageUrl!!,//enviado atravez del intent
                categoryTitle = currentCategory
                    ?: getString(R.string.cat_general) // Fallback si no hay categoría
            )
        }
    }

    // Función puramente visual: Recibe un booleano y cambia atributos del botón.
    private fun updateFavoriteIcon(isFavorite: Boolean) {
        if (isFavorite) {
            binding.btnFavorite.setIconResource(R.drawable.ic_favorite_filled)
            binding.btnFavorite.setIconTintResource(android.R.color.holo_red_dark)
        } else {
            binding.btnFavorite.setIconResource(R.drawable.ic_favorite_border)
            binding.btnFavorite.setIconTintResource(android.R.color.white)
        }
    }

    // --- TU METODO ROBUSTO DE COMPARTIR (Restaurado) ---
    private fun shareImageProcess() {
        // 1. Validación rápida en el Hilo Principal
        // NOTA: Ajusté 'imgFullDetail' a 'ivDetail' para coincidir con el XML nuevo
        val drawable = binding.ivDetail.drawable

        if (drawable == null) {
            Toast.makeText(this, getString(R.string.msg_imagen_no_lista), Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, getString(R.string.msg_preparando_envio), Toast.LENGTH_SHORT).show()

        // 2. Transición a Hilo Secundario (IO)
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // A. Convertir Drawable a Bitmap
                val bitmap = drawable.toBitmap()

                // B. Gestión de Caché
                val cachePath = File(cacheDir, "images")
                if (!cachePath.exists()) {
                    cachePath.mkdirs()
                }

                // Limpieza de archivos viejos
                cachePath.listFiles()?.forEach { image -> image.delete() }

                // C. Crear el archivo físico
                val newFile = File(cachePath, "share_img_${System.currentTimeMillis()}.jpg")
                val stream = FileOutputStream(newFile)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                stream.close()

                // D. Generar URI
                val authority = "${applicationContext.packageName}.provider"
                val contentUri = FileProvider.getUriForFile(
                    this@ImageDetailActivity,
                    authority,
                    newFile
                )

                // E. Volver al Hilo Principal para lanzar el Intent
                withContext(Dispatchers.Main) {
                    val shareIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        type = "image/jpeg"
                        putExtra(Intent.EXTRA_STREAM, contentUri)

                        // ClipData para Android 10+
                        clipData = ClipData.newRawUri(null, contentUri)

                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        putExtra(Intent.EXTRA_TEXT, getString(R.string.share_mensaje_extra))
                    }

                    startActivity(
                        Intent.createChooser(
                            shareIntent,
                            getString(R.string.share_titulo)
                        )
                    )
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        applicationContext,
                        getString(R.string.msg_error_procesar),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}