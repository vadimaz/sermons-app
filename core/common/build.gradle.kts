plugins {
    alias(libs.plugins.sermonindex.android.library)
    alias(libs.plugins.sermonindex.android.hilt)
}

android {
    namespace = "com.example.sermonindex.core.common"
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
}
