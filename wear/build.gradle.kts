plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.ogenrwotdaniel.cufflink.wear"
    compileSdk = 37

    defaultConfig {
        // Same applicationId as :mobile — see the note there.
        applicationId = "com.ogenrwotdaniel.cufflink"
        // Wear OS 4 (API 33) is the floor: it is where the granular READ_MEDIA_*
        // permissions begin, and every Galaxy Watch since the 4 has been updated past it.
        minSdk = 33
        targetSdk = 36
        // Offset by 1000 so phone and watch versionCodes can share one Play listing.
        versionCode = 1001
        versionName = "0.1.0"
    }

    buildTypes {
        // The release build is what goes onto the watch: R8-shrunk to a few MB, because an
        // unshrunk Compose debug APK is ~38 MB and Wi-Fi adb to a watch drops long transfers.
        // Signed with the debug key on purpose until a release key exists — the Data Layer
        // only links the phone and watch apps when both carry the same signature.
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    // Play Services drags in fragment 1.0; the ActivityResult APIs need >= 1.3.
    implementation(libs.androidx.fragment)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.wear.compose.material3)
    implementation(libs.wear.compose.foundation)
    implementation(libs.wear.compose.navigation)
    implementation(libs.play.services.wearable)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
}
