import org.jetbrains.compose.compose

plugins {
    kotlin("jvm") version "1.4.20-RC"
    id("org.jetbrains.compose") version "0.2.0-build128"
}

repositories {
    jcenter()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation("com.google.code.gson:gson:2.8.6")
}

compose.desktop {
    application {
        mainClass = "MainKt"
    }
}