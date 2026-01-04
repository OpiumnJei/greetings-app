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
        minSdk = 21
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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

    // Coil (Para cargar las imágenes en el Adapter más adelante)
    implementation("io.coil-kt:coil:2.6.0")
    // para las corrutinas
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    // ⭐ Agrega esta línea para que funcione el desugaring, para mantener compatibilidad con todos los dispositivos
    coreLibraryDesugaring(libs.desugar.jdk.libs)

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
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}