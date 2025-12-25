package com.greetingsapp.mobile.ui

import android.content.ClipData
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.lifecycleScope
import coil.load
import com.greetingsapp.mobile.R
import com.greetingsapp.mobile.databinding.ActivityImageDetailBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class ImageDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImageDetailBinding

    companion object {
        const val EXTRA_IMAGE_URL =
            "EXTRA_IMAGE_URL" //el valor de la constante debe coincidir con el enviado desde MainActivity
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //carga la imagen enviada desde el intent de MainActivity
        setupImageLoading()

        // cuando se presiona el boton de compartir se llama a shareImageProcess
        binding.btnShare.setOnClickListener {
            shareImageProcess()
        }
    }

    // metodo que carga la imagen
    private fun setupImageLoading() {
        val imageUrl = intent.getStringExtra(EXTRA_IMAGE_URL) ?: return

        val fixedUrl = imageUrl

        binding.imgFullDetail.load(fixedUrl) {
            crossfade(true)// Efecto visual suave
            allowHardware(false)
            placeholder(R.drawable.ic_launcher_background)
            error(R.drawable.ic_launcher_foreground)
        }
    }

    private fun shareImageProcess() {
        // 1. Validación rápida en el Hilo Principal
        val drawable = binding.imgFullDetail.drawable

        if (drawable == null) {
            Toast.makeText(this, "La imagen aún no carga", Toast.LENGTH_SHORT).show()
            return
        }

        // 2. Transición a Hilo Secundario (IO) para operaciones de disco
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // A. Convertir Drawable a Bitmap
                val bitmap = drawable.toBitmap()

                // B. GESTIÓN DE CACHÉ (La vivisección está abajo)
                val cachePath = File(cacheDir, "images")

                // Crear carpeta si no existe
                if (!cachePath.exists()){
                    cachePath.mkdirs()
                }

                // Limpieza: Borramos la imagen anterior para no llenar el teléfono del usuario
                cachePath.listFiles()?.forEach { image -> image.delete() }

                // C. Crear el archivo físico
                val newFile = File(cachePath, "share_img_${System.currentTimeMillis()}.jpg")
                val stream = FileOutputStream(newFile)

                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                stream.close()

                // D. Generar la "Llave de acceso" (URI)
                // Usamos applicationContext.packageName para evitar errores de BuildConfig
                val authority = "${applicationContext.packageName}.provider"
                val contentUri = FileProvider.getUriForFile(
                    this@ImageDetailActivity,
                    authority,
                    newFile
                )

                // E. Volver al Hilo Principal para la UI (Share Sheet)
                withContext(Dispatchers.Main) {
                    val shareIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        type = "image/jpeg"
                        putExtra(Intent.EXTRA_STREAM, contentUri)

                        // REQUISITO MODERNO (Android 10+):
                        // ClipData otorga permisos temporales de lectura a la app destino
                        clipData = ClipData.newRawUri(null, contentUri)

                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        putExtra(Intent.EXTRA_TEXT, "¡Mira este saludo! ✨")
                    }

                    startActivity(Intent.createChooser(shareIntent, "Compartir imagen..."))
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(applicationContext, "Error al procesar", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }
}