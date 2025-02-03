plugins {
    kotlin("jvm") version "2.1.10"
    `java-library`
    `maven-publish`
    signing
    id("io.spring.dependency-management") version "1.1.7"
}

repositories {
    mavenCentral()
}

val projVersion = project.findProperty("projVersion")
    ?.let {
        it as String
    }?.let {
        when(it.startsWith("v")) {
            true -> it.substring(1)
            else -> it
        }
    }

subprojects {
    val subProject = this
    val springBootVersion = "3.4.2"
    
    apply(plugin = "maven-publish")
    apply(plugin = "java-library")
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "signing")
    apply(plugin = "io.spring.dependency-management")
    
    group = "de.lise.fluxflow"
    version = projVersion ?: "0.2.0-SNAPSHOT-10"

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

    dependencyManagement {
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:$springBootVersion")
        }
    }

    dependencies {
        // Test
        testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
        testImplementation("org.assertj:assertj-core:3.27.3")
        testImplementation("org.mockito:mockito-inline:5.2.0")

        testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.4")
        testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.4")
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
            maven {
                name = "staging"
                url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
                credentials(PasswordCredentials::class)
            }
        }
        publications {
            create<MavenPublication>("snapshot") {
                groupId = subProject.group.toString()
                artifactId = subProject.name
                version = subProject.version.toString()

                from(components["java"])
            }
            
            create<MavenPublication>("maven") {
                groupId = subProject.group.toString()
                artifactId = subProject.name
                version = subProject.version.toString()

                from(components["java"])

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
        }
    }
    
    signing {
        sign(publishing.publications["maven"])
    }
}