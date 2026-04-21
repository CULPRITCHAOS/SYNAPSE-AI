plugins {
    id("synapse.kotlin.library")
}

dependencies {
    implementation(project(":core:common"))
    implementation(libs.kotlinx.serialization.json)
}
