package com.greetingsapp.mobile

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.greetingsapp.mobile.ads.AdManager

/**
 * Clase Application principal de la app.
 * Se usa para inicializar SDKs globales como AdMob.
 */
class GreetingsApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // ➕ Inicializar el SDK de AdMob
        MobileAds.initialize(this) { initializationStatus ->
            // Opcional: Puedes loguear el estado de cada adaptador de mediación
            initializationStatus.adapterStatusMap.forEach { (adapter, status) ->
                android.util.Log.d("AdMob", "Adapter: $adapter -> ${status.initializationState}")
            }

            // ➕ Pre-cargar el intersticial una vez que AdMob esté listo
            AdManager.loadInterstitial(this)
        }
    }
}
