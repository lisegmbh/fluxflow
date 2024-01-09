plugins {
    kotlin("jvm") version "1.9.21"
    `java-library`
    `maven-publish`
}


repositories {
    mavenCentral()
}

subprojects {
    val subProject = this
    
    apply(plugin = "maven-publish")
    apply(plugin = "java-library")
    apply(plugin = "org.jetbrains.kotlin.jvm")
    
    group = "de.lise.fluxflow"
    version = "0.0.1"

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
        publications {
            create<MavenPublication>("maven") {
                groupId = subProject.group.toString()
                artifactId = subProject.name
                version = subProject.version.toString()

                from(components["java"])
            }
        }
    }
}