plugins {
    id("io.github.idmosk.saga.kotlin-library-conventions")
    `java-test-fixtures`
    id("org.jetbrains.kotlinx.kover")
}

version = "0.1.0"

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.9.20")
    implementation(project(":spi-storage"))
    implementation(project(":spi-queue"))

    testImplementation(testFixtures(project(":spi-storage-test")))
    testImplementation(testFixtures(project(":spi-queue-test")))

    testFixturesImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    testFixturesImplementation(project(":spi-storage"))
    testFixturesImplementation(project(":spi-queue"))
    testFixturesImplementation(testFixtures(project(":spi-storage-test")))
    testFixturesImplementation(testFixtures(project(":spi-queue-test")))
}

mavenPublishing {
    pom {
        name.set("API")
        description.set("End user API")
        url.set("https://github.com/idmosk/saga-project/api")
    }
}
