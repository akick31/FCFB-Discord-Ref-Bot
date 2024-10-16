import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jlleitschuh.gradle.ktlint") version "10.1.0"
    kotlin("jvm") version "1.9.22"
    application
}

group = "org.example"
version = "1.0.0"
description = "FCFB-Discord-Ref-Bot"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.slf4j:slf4j-api:2.0.9")
    implementation("ch.qos.logback:logback-classic:1.4.12")
    implementation("io.ktor:ktor-server-core:2.3.0")
    implementation("io.ktor:ktor-server-netty:2.3.0")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.0")
    implementation("io.ktor:ktor-serialization-gson:2.3.0")
    implementation("io.ktor:ktor-client-gson:2.3.0")
    implementation("io.ktor:ktor-client-core:2.3.0")
    implementation("io.ktor:ktor-client-cio:2.3.0")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.0")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.0")
    implementation("io.ktor:ktor-serialization-jackson:2.3.0")
    implementation("dev.kord:kord-core:0.13.1")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.12.5")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.6.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

application {
    mainClass.set("com.fcfb.discord_ref_bot.FCFBDiscordRefBotKt")
}
