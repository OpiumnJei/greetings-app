package com.greetingsapp.mobile.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

/**
 * Singleton para manejar los anuncios intersticiales.
 * Pre-carga el anuncio en segundo plano para tenerlo listo.
 *
 * ⚠️ IMPORTANTE: Usa el Test Unit ID durante desarrollo.
 * Cambiar por el ID real antes de publicar.
 */
// 'object' significa que SOLO hay un AdManager en toda la app.
// No importa desde dónde lo llames, siempre es el mismo cocinero.
object AdManager {

    private const val TAG = "AdManager"

    // ⚠️ TEST UNIT ID - ¡Cambiar por    el real antes de publicar!
    private const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

    // En esta variable guardamos el anuncio descargado
    // El anuncio intersticial cargado (null si no está listo)
    private var interstitialAd: InterstitialAd? = null

    // Flag para saber si estamos cargando el anuncio
    private var isLoading = false

    /**
     * Pre-carga un anuncio intersticial en segundo plano.
     * Llamar desde GreetingsApp.onCreate() o cuando sea apropiado.
     */
    fun loadInterstitial(context: Context) {
        // Evitar cargas duplicadas, si ya se esta cargando un anuncion o hay uno cargado
        if (isLoading || interstitialAd != null) {
            Log.d(TAG, "Ya hay un anuncio cargado o cargando, saltando...")
            return
        }

        isLoading = true
        val adRequest = AdRequest.Builder().build() // se solicita el anuncio

        // Se le pide a Google que me mande el anuncio (esto tarda unos segundos)
        InterstitialAd.load(
            context,
            INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() { // Aviso cuando termine

                // ¡ÉXITO! el anuncio se mostro correctamente
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "✅ Intersticial cargado correctamente")
                    interstitialAd = ad // se guarda el anuncio en la varibale
                    isLoading = false
                }

                // FALLO al mostrar el anuncio
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "❌ Error al cargar intersticial: ${error.message}")
                    interstitialAd = null // la variable queda vacia
                    isLoading = false
                }
            }
        )
    }

    /**
     * Muestra el anuncio intersticial si está disponible.
     * @param activity La Activity desde donde se muestra el anuncio.
     * @param onAdDismissed Callback que se ejecuta cuando el usuario cierra el anuncio.
     */

    //Este metodo se llama cuando el contador de clicks llega a 3.
    fun showInterstitialIfReady(activity: Activity, onAdDismissed: () -> Unit = {}) {
        val ad = interstitialAd

        // se verifica si hay un anuncio
        if (ad == null) {
            Log.d(TAG, "⚠️ Intersticial no disponible, continuando sin anuncio...")
            onAdDismissed() // se deja pasar al usuario sin anuncio
            loadInterstitial(activity)  // Intentar cargar uno nuevo para la próxima vez
            return
        }

        // Si hay anuncio, se configura qué pasa cuando el usuario lo cierra
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {

            // Si hay anuncio, configuro qué pasa cuando el usuario lo cierra
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "👋 Usuario cerró el intersticial")
                interstitialAd = null //la variable que almacena el anuncio se hace null, para poder pre-cargar otro
                onAdDismissed() // Retornar a la otra pantalla
                loadInterstitial(activity)   // Cargar el siguiente anuncio
            }

            // Si falla al intentar mostrarse en pantalla
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.e(TAG, "❌ Error al mostrar intersticial: ${error.message}")
                interstitialAd = null
                onAdDismissed() // Dejo pasar al usuario
                loadInterstitial(activity) // Se intenta cargar otro
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "🎬 Intersticial mostrado")
            }
        }

        // Mostrar el anuncio
        ad.show(activity)
    }

    /**
     * Verifica si hay un anuncio listo para mostrar.
     */
    fun isInterstitialReady(): Boolean = (interstitialAd != null)
}
