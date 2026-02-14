plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // ➕ Agrega esta línea:
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.greetingsapp.mobile"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.greetingsapp.mobile"
        minSdk = 26  // Android 8.0 - Certificados SSL modernos soportados
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // 🔐 Configuración de firma para release
    signingConfigs {
        create("release") {
            storeFile = file("C:/Users/Jerlinson/Desktop/keystores/greetingsapp.jks")
            storePassword = "jg30111995"
            keyAlias = "greetings-release"
            keyPassword = "jg30111995"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true // ✅ Ofuscar y reducir código
            isShrinkResources = true // ✅ Eliminar recursos no usados
            signingConfig = signingConfigs.getByName("release") // 🔐 Usar firma release
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        // ⭐ Activa el soporte para librerías modernas
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true // <--- Agrega esta línea
    }
}

dependencies {
    // Retrofit y Gson (El puente a internet)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // Coil (Para cargar las imágenes)
    implementation("io.coil-kt:coil:2.6.0")

    // Corrutinas
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    // Desugaring para compatibilidad
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    implementation(libs.facebook.shimmer)
    // ➕ AdMob (Google Mobile Ads)
    implementation(libs.play.services.ads)
    // ➕ ROOM DATABASE
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler) // Ojo: aquí se usa "ksp", no "implementation"

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    testImplementation(libs.junit)
    testImplementation(libs.mockk) // ➕ Para tests unitarios
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}