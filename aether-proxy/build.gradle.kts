plugins {
    id("com.gradleup.shadow") version "9.0.0-beta4"
}

dependencies {
    api(project(":aether-common"))
    api(project(":aether-api"))
    
    implementation(libs.bundles.metrics)
    
    compileOnly("dev.waterdog.waterdogpe:waterdog:2.0.0-SNAPSHOT")
}

tasks.shadowJar {
    archiveClassifier.set("")
    mergeServiceFiles()
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
