plugins {
    alias(libs.plugins.weather.app.android.library)
    alias(libs.plugins.weather.app.android.library.jacoco)
    alias(libs.plugins.weather.app.hilt)
}

android {
    namespace = "jp.co.greensys.weather.app.core.common"
}

dependencies {
    implementation(projects.core.model)

    testImplementation(projects.core.testing)
}
