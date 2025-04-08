import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.allopen")
    id("com.google.devtools.ksp") version "2.1.0-1.0.29"
    id("io.micronaut.application") version "4.4.4"
    id("com.google.cloud.tools.jib") version "3.4.4"
}
version = "0.2"

group = "de.deutschepost.sdm.cdlib"
repositories {
    maven("https://artifactory.dhl.com/maven-remote")
}

micronaut {
    testRuntime("kotest5")
    processing {
        incremental(true)
        annotations("de.deutschepost.sdm.cdlib.*")
    }
}

val kotlinVersion = project.properties["kotlinVersion"] as String
val kotlinCoroutines = project.properties["kotlinCoroutines"] as String

dependencies {
    ksp("info.picocli:picocli-codegen")
    implementation("org.yaml:snakeyaml")
    implementation("info.picocli:picocli")
    implementation("io.micronaut:micronaut-http-client")
    implementation("io.micronaut.kotlin:micronaut-kotlin-runtime")
    implementation("io.micronaut.picocli:micronaut-picocli")
    implementation("io.micronaut:micronaut-retry")
    implementation("javax.annotation:javax.annotation-api") // todo remove?
    implementation("org.jetbrains.kotlin:kotlin-reflect:${kotlinVersion}")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${kotlinVersion}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${kotlinCoroutines}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:${kotlinCoroutines}")
    implementation("ch.qos.logback:logback-classic")
    implementation("io.micronaut:micronaut-jackson-databind")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // artifactory
    implementation("org.jfrog.artifactory.client:artifactory-java-client-services:2.19.1")
    // logging
    implementation("io.github.microutils:kotlin-logging:2.1.23")
    // webapproval sharepoint
    implementation("org.apache.httpcomponents:httpclient:4.5.14")
    implementation("org.codelibs:jcifs:1.3.18.3") // Never update this, Version 2 is incompatible
    // JGit (https://mvnrepository.com/artifact/org.eclipse.jgit/org.eclipse.jgit)
    implementation("org.eclipse.jgit:org.eclipse.jgit:7.1.0.202411261347-r")
    //publish metrics to azure cosmosdb
    implementation("com.azure:azure-identity")
    implementation("net.minidev:json-smart:2.5.2") // override bc vuln
    implementation("com.azure:azure-cosmos")

    // Tests only
    testImplementation("io.mockk:mockk")
    testImplementation("io.kotest:kotest-extensions-now")
}

application {
    mainClass.set("de.deutschepost.sdm.cdlib.CdlibCommand")
}

java {
    sourceCompatibility = JavaVersion.toVersion("21")
}

tasks {
    compileKotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }
    compileTestKotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }
    test {
        // https://github.com/mockk/mockk/issues/681
        jvmArgs(
            "--add-opens=java.base/java.time=ALL-UNNAMED",
            "--add-opens=java.base/java.util=ALL-UNNAMED",
            "--add-opens=java.base/java.lang=ALL-UNNAMED"
        )
        systemProperties = System.getProperties()
            .toList()
            .associate { it.first.toString() to it.second }
        systemProperties["kotest.tags"] = "UnitTest"

    }

    task<Test>("integrationTest") {
        // https://github.com/mockk/mockk/issues/681
        jvmArgs(
            "--add-opens=java.base/java.time=ALL-UNNAMED",
            "--add-opens=java.base/java.util=ALL-UNNAMED",
            "--add-opens=java.base/java.lang=ALL-UNNAMED"
        )
        description = "Runs integration tests."
        group = "verification"
        systemProperties = System.getProperties()
            .toList()
            .associate { it.first.toString() to it.second }
        systemProperties["kotest.tags"] = "IntegrationTest"

    }
//tag::jib[]
    jib {
        from.image = "docker.artifactory.dhl.com/cdlib/cdlib-base:latest"
        container.entrypoint = listOf("")
        extraDirectories {
            paths {
                path {
                    setFrom("scripts")
                    into = "/usr/bin"
                }
            }
            permissions.set(mapOf(
                "/usr/bin/cdlib" to "555",
            ))
        }
    }
//end::jib[]
}
