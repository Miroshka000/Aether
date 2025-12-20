plugins {
    id("java")
    id("com.github.node-gradle.node") version "7.0.2"
}

group = "miroshka.aether"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

node {
    version.set("20.11.0")
    npmVersion.set("10.2.4")
    download.set(true)
    nodeProjectDir.set(file("frontend"))
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":aether-common"))
    implementation(project(":aether-api"))

    implementation("io.javalin:javalin:6.3.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.2")
    implementation("com.auth0:java-jwt:4.4.0")
    implementation("org.slf4j:slf4j-simple:2.0.13")

    compileOnly("org.projectlombok:lombok:1.18.34")
    annotationProcessor("org.projectlombok:lombok:1.18.34")

    testImplementation(platform("org.junit:junit-bom:5.10.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}

val npmInstall = tasks.named("npmInstall")

val buildFrontend = tasks.register<com.github.gradle.node.npm.task.NpmTask>("buildFrontend") {
    dependsOn(npmInstall)
    args.set(listOf("run", "build"))
    workingDir.set(file("frontend"))
}

tasks.named("processResources") {
    dependsOn(buildFrontend)
}

tasks.register("devFrontend", com.github.gradle.node.npm.task.NpmTask::class) {
    dependsOn(npmInstall)
    args.set(listOf("run", "dev"))
    workingDir.set(file("frontend"))
}
