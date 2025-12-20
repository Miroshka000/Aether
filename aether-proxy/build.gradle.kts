plugins {
    id("com.gradleup.shadow") version "9.0.0-beta4"
    id("com.github.node-gradle.node") version "7.0.2"
}

node {
    version.set("20.11.0")
    npmVersion.set("10.2.4")
    download.set(true)
    nodeProjectDir.set(file("${project(":aether-web").projectDir}/frontend"))
}

repositories {
    mavenCentral()
}

dependencies {
    api(project(":aether-common"))
    api(project(":aether-api"))
    api(project(":aether-web"))
    
    implementation(libs.bundles.metrics)
    
    compileOnly("dev.waterdog.waterdogpe:waterdog:2.0.0-SNAPSHOT")
}

val npmInstall = tasks.named("npmInstall")

val buildFrontend = tasks.register<com.github.gradle.node.npm.task.NpmTask>("buildFrontend") {
    dependsOn(npmInstall)
    args.set(listOf("run", "build"))
    workingDir.set(file("${project(":aether-web").projectDir}/frontend"))
}

tasks.processResources {
    dependsOn(buildFrontend)
}

tasks.shadowJar {
    archiveClassifier.set("")
    mergeServiceFiles()
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
