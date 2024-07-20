plugins {
    // Apply the foojay-resolver plugin to allow automatic download of JDKs
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.4.0"
}

rootProject.name = "saga-project"
include("api")
include("api-spring-boot-starter-v2")
include("api-spring-boot-starter-v3")
include("spi-storage")
include("spi-queue")
include("spi-storage-test")
include("spi-queue-test")
