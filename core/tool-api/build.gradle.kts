plugins {
    id("synapse.kotlin.library")
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:apppack-api"))
    implementation(libs.kotlinx.serialization.json)
}
