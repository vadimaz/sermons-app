plugins {
    alias(libs.plugins.sermonindex.jvm.library)
}

dependencies {
    api(project(":core:model"))
    api(project(":core:domain"))

    api(libs.junit)
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.coroutines.test)
}
