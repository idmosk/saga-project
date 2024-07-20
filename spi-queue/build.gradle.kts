plugins {
    id("io.github.idmosk.saga.kotlin-library-conventions")
}

version = "0.1.0"

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
}

mavenPublishing {
    pom {
        name.set("Queues SPI")
        description.set("SPI you can implement")
        url.set("https://github.com/idmosk/saga-project/spi-queue")
    }
}
