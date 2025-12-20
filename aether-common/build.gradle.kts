plugins {
    id("java-library")
}

dependencies {
    api(libs.bundles.netty)
    api(libs.snappy)
    api(libs.snakeyaml)
    api(libs.slf4j.api)
    
    implementation(libs.bundles.logging)
}
