pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://repo.opencollab.dev/maven-releases/")
        maven("https://repo.opencollab.dev/maven-snapshots/")
    }
}

rootProject.name = "Aether"

include(":aether-common")
include(":aether-api")
include(":aether-proxy")
include(":aether-server")
include(":aether-web")
include(":aether-addons")