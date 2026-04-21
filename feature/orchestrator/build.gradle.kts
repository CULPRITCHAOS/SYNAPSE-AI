plugins {
    id("synapse.android.library")
    id("synapse.android.hilt")
}

android {
    namespace = "com.synapse.feature.orchestrator"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:model-api"))
    implementation(project(":core:apppack-api"))
    implementation(project(":core:tool-api"))
    
    implementation(libs.kotlinx.serialization.json)
}
