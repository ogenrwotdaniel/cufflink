plugins {
    alias(libs.plugins.android.library)
}

// Everything both ends of the wire must agree on: the channel path, the header
// that rides in front of every file, and the pure rules about names and kinds.
// One module instead of two hand-kept copies — the drift between copies is the
// bug this module exists to make impossible.
android {
    namespace = "com.ogenrwotdaniel.cufflink.shared"
    compileSdk = 37
    defaultConfig { minSdk = 29 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    api(libs.play.services.wearable)
    api(libs.kotlinx.coroutines.android)
    api(libs.kotlinx.coroutines.play.services)
    testImplementation(libs.junit)
}
