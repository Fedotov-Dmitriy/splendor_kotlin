plugins {
    kotlin("jvm") version "1.9.24"
    application
}

group = "splendor"
version = "1.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    sourceSets {
        main {
            kotlin.srcDirs("src/domain", "src/service", "src/gui", "src/main")
        }
        test {
            kotlin.srcDirs("test/domain", "test/service", "test/system")
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "21"
}

application {
    mainClass.set((findProperty("mainClass") as String?) ?: "gui.GuiMainKt")
}
