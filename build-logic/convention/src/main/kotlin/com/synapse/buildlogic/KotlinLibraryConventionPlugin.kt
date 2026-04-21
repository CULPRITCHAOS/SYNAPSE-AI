package com.synapse.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

class KotlinLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("org.jetbrains.kotlin.jvm")
            }

            extensions.configure<JavaPluginExtension>("java") {
                toolchain {
                    languageVersion.set(JavaLanguageVersion.of(17))
                }
            }

            extensions.configure<KotlinJvmProjectExtension>("kotlin") {
                explicitApi()
            }
        }
    }
}
