import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    id("org.jlleitschuh.gradle.ktlint") version "12.1.0"
    kotlin("jvm") version "2.0.21"
    application
}

sourceSets {
    main {
        resources.srcDir("src/main/resources")
    }
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
        force("org.slf4j:slf4j-api:2.0.14")
        force("org.slf4j:slf4j-log4j12:2.0.14")
    }
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.slf4j:slf4j-log4j12:2.0.14")
    implementation("io.ktor:ktor-server-core:2.3.12")
    implementation("io.ktor:ktor-server-netty:2.3.12")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.12")
    implementation("io.ktor:ktor-serialization-gson:2.3.12")
    implementation("io.ktor:ktor-client-gson:2.3.12")
    implementation("io.ktor:ktor-client-core:2.3.12")
    implementation("io.ktor:ktor-client-cio:2.3.12")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.12")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")
    implementation("io.ktor:ktor-serialization-jackson:2.3.12")
    implementation("dev.kord:kord-core:0.14.0")
    implementation("com.kotlindiscord.kord.extensions:kord-extensions:1.6.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.0")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.0.21")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("io.github.oshai:kotlin-logging-jvm:4.0.0")
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

tasks.withType<Copy> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.jar {
    manifest.attributes["Main-Class"] = "com.fcfb.discord.refbot.FCFBDiscordRefBotKt"
    val dependencies =
        configurations
            .runtimeClasspath
            .get()
            .map(::zipTree)
    from(dependencies)
    from(sourceSets.main.get().resources.srcDirs)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

application {
    mainClass.set("com.fcfb.discord.refbot.FCFBDiscordRefBotKt")
}
