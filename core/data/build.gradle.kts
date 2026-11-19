plugins {
    alias(libs.plugins.sermonindex.android.library)
    alias(libs.plugins.sermonindex.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.sermonindex.core.data"
}

dependencies {
    api(project(":core:model"))
    api(project(":core:domain"))
    api(project(":core:common"))
    api(project(":core:network"))
    api(project(":core:database"))
    api(project(":core:datastore"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
