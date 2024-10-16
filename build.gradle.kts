import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    id("org.jlleitschuh.gradle.ktlint") version "12.1.0"
    kotlin("jvm") version "2.0.21"
    application
}

group = "com.fcfb"
version = "1.0.0"
description = "FCFB-Discord-Ref-Bot"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()

    maven {
        name = "Sonatype Snapshots (Legacy)"
        url = uri("https://oss.sonatype.org/content/repositories/snapshots")
    }

    maven {
        name = "Sonatype Snapshots"
        url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots")
    }
}

configurations.all {
    resolutionStrategy {
        force("org.slf4j:slf4j-api:1.7.36")
        force("ch.qos.logback:logback-classic:1.2.13")
        force("io.ktor:ktor-client-core:2.3.12")
        force("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
    }
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("ch.qos.logback:logback-classic:1.2.13")
    implementation("io.ktor:ktor-server-core:2.3.11")
    implementation("io.ktor:ktor-server-netty:2.3.11")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.11")
    implementation("io.ktor:ktor-serialization-gson:2.3.11")
    implementation("io.ktor:ktor-client-gson:2.3.11")
    implementation("io.ktor:ktor-client-core:2.3.11")
    implementation("io.ktor:ktor-client-cio:2.3.11")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.11")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.11")
    implementation("io.ktor:ktor-serialization-jackson:2.3.11")
    implementation("dev.kord:kord-core:0.14.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.16.1")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.0.21")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
    }
}
tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.fcfb.discord.refbot.FCFBDiscordRefBotKt"
    }
}

application {
    mainClass.set("com.fcfb.discord.refbot.FCFBDiscordRefBotKt")
}
