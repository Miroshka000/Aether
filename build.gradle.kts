plugins {
    id("java-library")
}

allprojects {
    group = "miroshka.aether"
    version = "1.0.1"
    
    repositories {
        mavenCentral()
        maven("https://repo.opencollab.dev/maven-releases/")
        maven("https://repo.opencollab.dev/maven-snapshots/")
        maven("https://repo.waterdog.dev/main")
        maven("https://jitpack.io")
    }
}

subprojects {
    apply(plugin = "java-library")
    
    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }
    
    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
    
    dependencies {
        compileOnly("org.projectlombok:lombok:1.18.36")
        annotationProcessor("org.projectlombok:lombok:1.18.36")
        
        testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
        testImplementation("org.mockito:mockito-core:5.14.2")
    }
    
    tasks.test {
        useJUnitPlatform()
    }
}
