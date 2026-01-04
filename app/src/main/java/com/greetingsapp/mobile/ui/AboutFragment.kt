package com.greetingsapp.mobile.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.greetingsapp.mobile.BuildConfig
import com.greetingsapp.mobile.R
import com.greetingsapp.mobile.databinding.FragmentAboutBinding

class AboutFragment : Fragment() {

    //toggles variables
    private var _binding: FragmentAboutBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAboutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupVersionInfo()
        setupButtons()
    }

    private fun setupVersionInfo() {
        // ⭐ TIP: Usa strings.xml con argumentos para facilitar la traducción
        // En strings.xml: <string name="app_version">Versión %s</string>
        // binding.tvAppVersion.text = getString(R.string.app_version, BuildConfig.VERSION_NAME)
        binding.tvAppVersion.text = getString(R.string.about_version, BuildConfig.VERSION_NAME)
    }

    private fun setupButtons() {
        with(binding) {
            // 1. Email de Contacto
            btnContact.setOnClickListener {
                sendEmail(
                    address = "soporte@saludosdiarios.app",
                    subject = getString(R.string.email_subject_contact)
                )
            }

            // 2. Reporte de Bug (Con datos técnicos)
            btnReportBug.setOnClickListener {
                val deviceInfo = """
                    
                    ---
                    Dispositivo: ${Build.MANUFACTURER} ${Build.MODEL}
                    Android OS: ${Build.VERSION.RELEASE} API ${Build.VERSION.SDK_INT}
                    App Version: ${BuildConfig.VERSION_NAME}
                """.trimIndent()

                sendEmail(
                    address = "bugs@saludosdiarios.app",
                    subject = getString(R.string.email_subject_bug),
                    body = "Describe el problema aquí:\n$deviceInfo"
                )
            }

            // 3. URLs Simples
            btnDonate.setOnClickListener { openUrl("https://paypal.me/tuusuario") }
            btnPrivacy.setOnClickListener { openUrl("https://saludosdiarios.app/privacidad") }

            // 4. Redes Sociales (Lógica inteligente)

            btnLinkedin.setOnClickListener {
                openSocialMedia(
                    appUri = "https://www.linkedin.com/in/jerlinson-gonzalez-a85a6720b/",
                    webUri = "https://www.linkedin.com/in/jerlinson-gonzalez-a85a6720b/",
                    packageName = "com.linkedin.android"
                )
            }
            btnInstagram.setOnClickListener {
                openSocialMedia(
                    appUri = "http://instagram.com/_u/saludosdiarios.app", //_u abre el perfil del usuario PERO EN LA APP de ig no en la web :)
                    webUri = "https://instagram.com/saludosdiarios.app",
                    packageName = "com.instagram.android"
                )
            }

            btnTwitter.setOnClickListener {
                openSocialMedia(
                    // Esto no es un enlace web estándar. Es un Deep Link (enlace profundo) o Custom Scheme.
                    // * twitter -> Esquema (Protocolo).
                    // * user -> Host (Acción).
                    // * screen_name -> Parámetro.
                    appUri = "twitter://user?screen_name=saludosdiarios",
                    webUri = "https://twitter.com/saludosdiarios", // uri del enlace al perfil en la web(caso que el usuario no tenga la app instalada)
                    // packageName es el identificador unico de la app de twitter usando internamente por Google Play para referirse a la app
                    packageName = "com.twitter.android" // O com.twitter.android.lite
                )
            }
        }
    }

    // --- FUNCIONES DE UTILIDAD (HELPER FUNCTIONS) ---

    // helper function encargada de enviar correos
    private fun sendEmail(address: String, subject: String, body: String = "") {
        val intent = Intent(Intent.ACTION_SENDTO).apply { // Al usar ACTION_SENDTO con el dato mailto lanzamos unicamente aplicaciones de correo electrónico.
            data = Uri.parse("mailto:") // solo muestra apps que sepan manejar el protocolo mailto
            putExtra(Intent.EXTRA_EMAIL, arrayOf(address)) // envia la direccion/es de correo que rellenara el campo del correo dentro de la aplicacion de mensajeria
            putExtra(Intent.EXTRA_SUBJECT, subject) // lo mismo -> asunto
            if (body.isNotEmpty()) {
                putExtra(Intent.EXTRA_TEXT, body) //cuerpo del correo
            }
        }
        /// con Intent.createChooser forzamos a que aparezca el menu de seleccion de apps Bottom Sheet
        launchIntentSafe(Intent.createChooser(intent, getString(R.string.email_chooser_title))) // "Enviar correo"-> titulo personalizado
    }

    // helper function encargada de abrir urls externas
    private fun openUrl(url: String) {
        launchIntentSafe(Intent(Intent.ACTION_VIEW, Uri.parse(url))) // action_iew se usa para abrir/mostrar URIs, por ejemplo https://facebook.com
    }

    // helper function encargada de abrir aplicaciones relacionadas a redes de sociales
    private fun openSocialMedia(appUri: String, webUri: String, packageName: String) {
        try {
            // Intentar abrir la app nativa
            val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse(appUri))
            appIntent.setPackage(packageName) // se lanza la app que se indique mediante su identificador(packageName)
            startActivity(appIntent)
        } catch (e: Exception) {
            // Fallback: Abrir en navegador si la app no está instalada
            openUrl(webUri)
        }
    }

    // 🔑 DRY: Manejo de errores centralizado para evitar repetir try-catch
    private fun launchIntentSafe(intent: Intent) {
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(requireContext(), getString(R.string.error_no_app), Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), getString(R.string.error_open_app), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}