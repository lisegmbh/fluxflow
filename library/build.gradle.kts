plugins {
    kotlin("jvm") version "1.9.21"
    `java-library`
    `maven-publish`
}


repositories {
    mavenCentral()
}

val projVersion = project.findProperty("projVersion")

subprojects {
    val subProject = this
    
    apply(plugin = "maven-publish")
    apply(plugin = "java-library")
    apply(plugin = "org.jetbrains.kotlin.jvm")
    
    group = "de.lise.fluxflow"
    version = projVersion ?: "0.0.1"

    repositories {
        mavenCentral()
    }

    java {
        withSourcesJar()
        withJavadocJar()
        sourceCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        jvmToolchain(17)
    }

    dependencies {
        // Test
        testImplementation("org.mockito.kotlin:mockito-kotlin:3.2.0")
        testImplementation("org.assertj:assertj-core:3.24.2")
        testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.3")
        testImplementation("org.mockito:mockito-inline:5.2.0")

        testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.3")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    publishing {
        repositories {
            maven {
                name = "snapshot"
                url = uri("https://nexus.cloud.lise.de/repository/maven-public/")
                credentials(PasswordCredentials::class)
            }
        }
        publications {
            create<MavenPublication>("maven") {
                groupId = subProject.group.toString()
                artifactId = subProject.name
                version = subProject.version.toString()

                from(components["java"])

                pom {
                    url.set("https://github.com/lisegmbh/fluxflow")
                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                    scm {
                        connection.set("scm:git:git://github.com/lisegmbh/fluxflow.git")
                        developerConnection.set("scm:git:ssh://git@github.com:lisegmbh/fluxflow.git")
                        url.set("https://github.com/lisegmbh/fluxflow")
                    }
                    developers {
                        developer {
                            id.set("lisegmbh")
                            name.set("lise GmbH")
                            email.set("support@lise.de")
                        }
                    }
                }
            }
        }
    }
}