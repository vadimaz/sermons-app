plugins {
    alias(libs.plugins.sermonindex.android.library)
    alias(libs.plugins.sermonindex.android.hilt)
}

android {
    namespace = "com.example.sermonindex.core.media"
}

dependencies {
    api(project(":core:model"))
    api(project(":core:domain"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.core.ktx)
    api(libs.media3.exoplayer)
    api(libs.media3.session)
    api(libs.media3.ui)
    api(libs.media3.ui.compose)
    implementation(libs.kotlinx.coroutines.core)
}
