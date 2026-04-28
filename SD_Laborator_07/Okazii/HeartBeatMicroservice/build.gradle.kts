plugins {
    id("com.gradleup.shadow")
}

dependencies {
    implementation(project(":MessageLibrary"))
    implementation(kotlin("stdlib"))
    implementation("io.reactivex.rxjava3:rxkotlin:3.0.0")
    implementation("io.reactivex.rxjava3:rxjava:3.1.8")
}

tasks.shadowJar {
    archiveBaseName.set("HeartBeatMicroservice")
    archiveClassifier.set("")
    archiveVersion.set("")
    manifest {
        attributes["Main-Class"] = "HeartBeatMicroserviceKt"
    }
}

tasks.build { dependsOn(tasks.shadowJar) }
