plugins {
    kotlin("jvm") version "1.9.23" apply false
    id("com.gradleup.shadow") version "8.3.3" apply false
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    repositories {
        mavenCentral()
    }

    tasks.withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            jvmTarget = "21"
        }
    }

    // Tell Gradle that sources are in src/ (IntelliJ-style flat layout)
    the<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension>()
        .sourceSets.getByName("main").kotlin.srcDirs("src")
}
