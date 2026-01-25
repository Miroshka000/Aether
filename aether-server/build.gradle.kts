plugins {
    id("java-library")
    id("org.allaymc.gradle.plugin") version "0.2.1"
    id("com.gradleup.shadow") version "9.0.0-beta4"
}

repositories {
    mavenCentral()
}

allay {
    api = "0.23.0"
    apiOnly = false
    generatePluginDescriptor = true
    
    plugin {
        name = "Aether"
        entrance = "miroshka.aether.server.AetherServerPlugin"
        version = project.version.toString()
        authors += "Miroshka"
        website = "https://github.com/Miroshka000/Aether"
        dependency("PlaceholderAPI")
    }
}

dependencies {
    api(project(":aether-common"))
    api(project(":aether-api"))
    
    implementation(libs.bundles.metrics)
    
    compileOnly(group = "org.allaymc", name = "papi", version = "0.1.1")
    compileOnly("net.luckperms:api:5.4")
}

tasks.shadowJar {
    archiveClassifier.set("")
    mergeServiceFiles()
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
