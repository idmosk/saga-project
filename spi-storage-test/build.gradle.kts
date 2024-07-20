plugins {
    id("io.github.idmosk.saga.kotlin-library-conventions")
    `java-test-fixtures`
    id("org.jetbrains.kotlinx.kover")
}

version = "0.1.0"

dependencies {
    implementation(project(":spi-storage"))
    implementation(kotlin("test-junit5"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    testFixturesImplementation(project(":spi-storage"))
}

mavenPublishing {
    pom {
        name.set("Tests for storage")
        description.set("Tests for your SPI implementation")
        url.set("https://github.com/idmosk/saga-project/spi-storage-test")
    }
}
