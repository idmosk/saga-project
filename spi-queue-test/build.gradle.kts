plugins {
    id("io.github.idmosk.saga.kotlin-library-conventions")
    `java-test-fixtures`
    id("org.jetbrains.kotlinx.kover")
}

version = "0.1.0"

dependencies {
    implementation(project(":spi-queue"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    implementation(kotlin("test-junit5"))

    testFixturesImplementation(project(":spi-queue"))
    testFixturesImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
}

mavenPublishing {
    pom {
        name.set("Tests for queue")
        description.set("Tests for your SPI implementation")
        url.set("https://github.com/idmosk/saga-project/spi-queue-test")
    }
}
