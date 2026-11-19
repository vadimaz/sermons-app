plugins {
    alias(libs.plugins.sermonindex.android.feature)
}

android {
    namespace = "com.example.sermonindex.feature.speakers"
}

dependencies {
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)
}
