import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    `java-gradle-plugin`
}

kotlin {
    compilerOptions {
        languageVersion.set(KotlinVersion.KOTLIN_2_2)
        apiVersion.set(KotlinVersion.KOTLIN_2_2)
    }
}

dependencies {
    implementation("org.ow2.asm:asm:9.9.1")
    implementation(project(":core:reflection")) {
        isTransitive = false
    }
    implementation(project(":core:stereotyped")) {
        isTransitive = false
    }

    testImplementation(gradleTestKit())
}

gradlePlugin {
    plugins {
        create("fluxflowTypeManifest") {
            id = "de.lise.fluxflow.type-manifest"
            implementationClass = "de.lise.fluxflow.gradle.manifest.FluxFlowTypeManifestPlugin"
        }
    }
}

tasks.test {
    val stereotypedJar = project(":core:stereotyped").tasks.named<Jar>("jar")
    dependsOn(stereotypedJar)
    systemProperty("fluxflow.test.stereotypedJar", stereotypedJar.flatMap { it.archiveFile }.get().asFile)
}
