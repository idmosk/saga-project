plugins {
    id("io.github.idmosk.saga.kotlin-library-conventions")
    id("org.jetbrains.kotlinx.kover")
    id("io.spring.dependency-management") version "1.1.5"
    kotlin("kapt")
}

group = "io.github.idmosk.saga.spring-boot-3"
version = "0.1.0"
val title = "api-spring-boot-starter"
val springBootVersion = "3.3.0"

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:$springBootVersion")
    }
}

dependencies {
    compileOnly("org.springframework.boot:spring-boot-starter")
    api(project(":api"))
    api(project(":spi-storage"))
    api(project(":spi-queue"))

    kapt("org.springframework.boot:spring-boot-configuration-processor")
    kapt("org.springframework.boot:spring-boot-autoconfigure-processor")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    annotationProcessor("org.springframework.boot:spring-boot-autoconfigure-processor")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
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

tasks.named("compileJava") {
    inputs.files(tasks.named("processResources"))
}

mavenPublishing {
    pom {
        name.set("Spring Boot Starter for API")
        description.set("Spring boot starter for API. Spring boot v3")
        url.set("https://github.com/idmosk/saga-project/api-spring-boot-starter-v3")
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

kotlin {
    jvmToolchain(17)
}

// https://github.com/Kotlin/dokka/issues/3472
configurations.matching { it.name.startsWith("dokka") }.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group.startsWith("com.fasterxml.jackson")) {
            useVersion("2.15.3")
        }
    }
}
