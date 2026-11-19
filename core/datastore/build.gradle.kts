plugins {
    alias(libs.plugins.sermonindex.android.library)
    alias(libs.plugins.sermonindex.android.hilt)
}

android {
    namespace = "com.example.sermonindex.core.datastore"
}

dependencies {
    api(project(":core:model"))
    api(project(":core:domain"))
    implementation(project(":core:common"))

    api(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
