plugins {
    id("io.github.idmosk.saga.kotlin-library-conventions")
    id("org.jetbrains.kotlinx.kover")
    id("io.spring.dependency-management") version "1.1.5"
    kotlin("kapt")
}

group = "io.github.idmosk.saga.spring-boot-2"
version = "0.1.0"
val title = "api-spring-boot-starter"
val springBootVersion = "2.7.18"

dependencies {
    compileOnly("org.springframework.boot:spring-boot-starter:$springBootVersion")
    api(project(":api"))
    api(project(":spi-storage"))
    api(project(":spi-queue"))

    kapt("org.springframework.boot:spring-boot-configuration-processor:$springBootVersion")
    kapt("org.springframework.boot:spring-boot-autoconfigure-processor:$springBootVersion")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor:$springBootVersion")
    annotationProcessor("org.springframework.boot:spring-boot-autoconfigure-processor:$springBootVersion")

    testImplementation("org.springframework.boot:spring-boot-starter-test:$springBootVersion")
    testImplementation(testFixtures(project(":api")))
    testImplementation(testFixtures(project(":spi-storage-test")))
    testImplementation(testFixtures(project(":spi-queue-test")))
}

base {
    archivesName.set(title)
}

tasks.jar {
    manifest {
        attributes(
            mapOf(
                "Implementation-Title" to title,
                "Implementation-Group" to project.group,
                "Implementation-Version" to project.version,
            ),
        )
    }
}

tasks.compileJava {
    inputs.files(tasks.named("processResources"))
}

mavenPublishing {
    pom {
        name.set("Spring Boot Starter for API")
        description.set("Spring boot starter for API. Spring boot v2")
        url.set("https://github.com/idmosk/saga-project/api-spring-boot-starter-v2")
    }
}

kapt {
    arguments {
        arg(
            "org.springframework.boot.configurationprocessor.additionalMetadataLocations",
            "$projectDir/src/main/resources",
        )
    }
}
