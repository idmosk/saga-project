import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.SonatypeHost
import javax.xml.parsers.DocumentBuilderFactory

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

afterEvaluate {
    if (tasks.findByName("koverXmlReport") != null) {
        tasks.register("printLineCoverage") {
            group = "verification" // Put into the same group as the `kover` tasks
            dependsOn("koverXmlReport")
            doLast {
                val report = file("$buildDir/reports/kover/report.xml")

                val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(report)
                val rootNode = doc.firstChild
                var childNode = rootNode.firstChild

                var coveragePercent = 0.0

                while (childNode != null) {
                    if (childNode.nodeName == "counter") {
                        val typeAttr = childNode.attributes.getNamedItem("type")
                        if (typeAttr.textContent == "LINE") {
                            val missedAttr = childNode.attributes.getNamedItem("missed")
                            val coveredAttr = childNode.attributes.getNamedItem("covered")

                            val missed = missedAttr.textContent.toLong()
                            val covered = coveredAttr.textContent.toLong()

                            coveragePercent = (covered * 100.0) / (missed + covered)

                            break
                        }
                    }
                    childNode = childNode.nextSibling
                }

                println("%.1f".format(coveragePercent))
            }
        }
    }
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
