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
    maven("https://repo.nethergames.org/repository/NetherGamesMC/")
    maven("https://jitpack.io")
    mavenLocal()
}

dependencies {
    api(project(":aether-common"))
    api(project(":aether-api"))
    api(project(":aether-web"))
    
    implementation(libs.bundles.metrics)
    
    implementation("com.github.NetherGamesMC:ProxyTransport:2.0.6")
    implementation("com.github.luben:zstd-jni:1.5.5-4")
    implementation("io.netty.incubator:netty-incubator-codec-native-quic:0.0.62.Final:linux-x86_64")
    
    runtimeOnly("org.slf4j:slf4j-simple:2.0.16")
    
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
