plugins {
    `kotlin-dsl`
}

group = "com.synapse.buildlogic"

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("kotlinLibrary") {
            id = "synapse.kotlin.library"
            implementationClass = "com.synapse.buildlogic.KotlinLibraryConventionPlugin"
        }
        register("androidLibrary") {
            id = "synapse.android.library"
            implementationClass = "com.synapse.buildlogic.AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = "synapse.android.compose"
            implementationClass = "com.synapse.buildlogic.AndroidComposeConventionPlugin"
        }
        register("androidHilt") {
            id = "synapse.android.hilt"
            implementationClass = "com.synapse.buildlogic.AndroidHiltConventionPlugin"
        }
    }
}
