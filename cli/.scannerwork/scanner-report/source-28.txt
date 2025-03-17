rootProject.name = "cli"
pluginManagement {
    val kotlinVersion: String by settings
    plugins {
        id("org.jetbrains.kotlin.jvm").version(kotlinVersion)
        id("org.jetbrains.kotlin.plugin.allopen").version(kotlinVersion)
    }
    repositories {
        maven("https://artifactory.dhl.com/maven-remote")
    }
}
