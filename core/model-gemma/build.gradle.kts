plugins {
    id("synapse.android.library")
    id("synapse.android.hilt")
}

android {
    namespace = "com.synapse.core.model.gemma"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:model-api"))
    
    implementation(libs.google.mediapipe.genai)
    implementation(libs.kotlinx.serialization.json)
}
