plugins {
    alias(libs.plugins.sermonindex.android.library)
    alias(libs.plugins.sermonindex.android.library.compose)
}

android {
    namespace = "com.example.sermonindex.core.ui"
}

dependencies {
    api(project(":core:model"))
    api(project(":core:media"))

    implementation(libs.androidx.core.ktx)
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.material.icons.extended)
    api(libs.coil.compose)

    testImplementation(project(":core:testing"))
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
