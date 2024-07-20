plugins {
    id("io.github.idmosk.saga.kotlin-library-conventions")
}

version = "0.1.0"

dependencies {
    implementation("com.fasterxml.uuid:java-uuid-generator:5.0.0")
}

mavenPublishing {
    pom {
        name.set("Storages SPI")
        description.set("SPI you can implement")
        url.set("https://github.com/idmosk/saga-project/spi-storage")
    }
}
