plugins {
    alias(libs.plugins.sermonindex.android.library)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.sermonindex.core.navigation"
}

dependencies {
    api(libs.androidx.navigation3.runtime)
    implementation(libs.kotlinx.serialization.json)
}
