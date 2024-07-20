import com.vanniktech.maven.publish.SonatypeHost
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.JavadocJar

plugins {
    id("io.github.idmosk.saga.kotlin-common-conventions")

    `java-library`
    `maven-publish`
    id("com.vanniktech.maven.publish")
}

tasks.javadoc {
    if (JavaVersion.current().isJava9Compatible) {
        (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
    }
}

tasks.register<Jar>("dokkaHtmlJar") {
    dependsOn(tasks.dokkaHtml)
    from(tasks.dokkaHtml.flatMap { it.outputDirectory })
    archiveClassifier.set("kdoc")
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)

    afterEvaluate {
        coordinates(artifactId = tasks.jar.get().archiveBaseName.get())
    }

    pom {
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                name.set("Dmitry Moskalev")
                email.set("moscalevd@gmail.com")
            }
        }
        scm {
            url.set("http://github.com/idmosk/saga-project/tree/master")
            connection.set("scm:git:git://github.com/idmosk/saga-project.git")
            developerConnection.set("scm:git:ssh://github.com/idmosk/saga-project.git")
        }
    }

    configure(KotlinJvm(
        javadocJar = JavadocJar.Dokka("dokkaHtml"),
        sourcesJar = true,
    ))

    signAllPublications()
}
