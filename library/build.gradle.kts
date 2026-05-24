plugins {
    kotlin("jvm") version "2.3.10"
    id("org.jetbrains.dokka") version "2.2.0"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.vanniktech.maven.publish") version "0.36.0"
}

repositories {
    mavenCentral()
}

val projVersion = project.findProperty("projVersion")
    ?.let {
        it as String
    }?.let {
        when (it.startsWith("v")) {
            true -> it.substring(1)
            else -> it
        }
    }

val intermediateProjectPaths = setOf(":core", ":springboot")

subprojects {
    val subProject = this
    val springBootVersion = "3.5.7"

    if (intermediateProjectPaths.contains(subProject.path)) {
        println("Intermediate sub project ${subProject.path} is skipped.")
        return@subprojects
    }

    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "com.vanniktech.maven.publish")
    apply(plugin = "org.jetbrains.dokka")

    group = "de.lise.fluxflow"
    version = projVersion ?: "0.3.0-SNAPSHOT-6"
    
    repositories {
        mavenCentral()
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_17
    }

    tasks.withType<Javadoc>().configureEach {
        // disable plain JavaDoc (fails with Kotlin sources)
        enabled = false
    }

    kotlin {
        jvmToolchain(17)
    }

    dependencyManagement {
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:$springBootVersion") {
                bomProperties(
                    mapOf("kotlin.version" to "2.2.0")
                )
            }
        }
    }

    dependencies {
        // Test
        testImplementation("org.mockito.kotlin:mockito-kotlin:6.3.0")
        testImplementation("org.assertj:assertj-core:3.27.7")
        testImplementation("org.mockito:mockito-inline:5.2.0")

        testImplementation(platform("org.junit:junit-bom:6.1.0"))
        testImplementation("org.junit.jupiter:junit-jupiter")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
    
    mavenPublishing {
        publishToMavenCentral()
        signAllPublications()

        coordinates(
            subProject.group.toString(),
            subProject.name,
            subProject.version.toString()
        )

        pom {
            name.set(subProject.name)
            description.set(
                subProject.description
                    ?: "A flexible workflow engine that helps to create and orchestrate business processes using domain code."
            )
            url.set("https://github.com/lisegmbh/fluxflow")
            licenses {
                license {
                    name.set("The Apache License, Version 2.0")
                    url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                }
            }
            scm {
                connection.set("scm:git:git://github.com/lisegmbh/fluxflow.git")
                developerConnection.set("scm:git:ssh://github.com:lisegmbh/fluxflow.git")
                url.set("https://github.com/lisegmbh/fluxflow")
            }
            developers {
                developer {
                    id.set("bobmazy")
                    name.set("Christian Scholz")
                    email.set("christian.scholz@lise.de")
                    organization.set("lise GmbH")
                    organizationUrl.set("https://lise.de")
                    url.set("https://github.com/bobmazy")
                }
                developer {
                    id.set("DerPipo")
                    name.set("Dominik Alexander")
                    email.set("dominik.alexander@lise.de")
                    organization.set("lise GmbH")
                    organizationUrl.set("https://lise.de")
                    url.set("https://github.com/DerPipo")
                }
                developer {
                    id.set("jagadish-singh-lise")
                    name.set("Jagadish Singh")
                    email.set("jagadish.singh@lise.de")
                    organization.set("lise GmbH")
                    organizationUrl.set("https://lise.de")
                    url.set("https://github.com/jagadish-singh-lise")
                }
                developer {
                    id.set("masinger")
                    name.set("Marcel Singer")
                    email.set("marcel.singer@live.de")
                    organization.set("lise Gmbh")
                    organizationUrl.set("https://lise.de")
                    url.set("https://github.com/masinger")
                }
            }
        }

    }
    publishing {
        repositories {
            maven {
                name = "snapshot"
                url = uri("https://nexus.cloud.lise.de/repository/maven-public/")
                credentials(PasswordCredentials::class)
            }
        }
    }
}