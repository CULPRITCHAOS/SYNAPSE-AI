plugins {
    id("synapse.android.library")
    id("synapse.android.hilt")
}

android {
    namespace = "com.synapse.core.tool.system"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:apppack-api"))
    implementation(project(":core:tool-api"))
    
    implementation(libs.kotlinx.serialization.json)
}
