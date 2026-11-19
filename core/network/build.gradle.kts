plugins {
    alias(libs.plugins.sermonindex.android.library)
    alias(libs.plugins.sermonindex.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.sermonindex.core.network"
}

dependencies {
    api(project(":core:model"))
    api(project(":core:domain"))
    implementation(project(":core:common"))

    api(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    api(libs.okhttp)
    implementation(libs.okhttp.logging)
    api(libs.kotlinx.serialization.json)
    implementation(libs.androidx.core.ktx)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
