import de.lise.fluxflow.gradle.SecurityTestRequirements

plugins {
    id("fluxflow.security-tests")
}

extensions.configure<SecurityTestRequirements>("securityTests") {
    requiredClasses.set(listOf(
        "de.lise.fluxflow.mongo.security.baseline.SecurityWitnessTest",
        "de.lise.fluxflow.mongo.security.baseline.ActivationBaselineTest",
        "de.lise.fluxflow.mongo.security.production.Boot4ProductionMongoSecurityIT",
        "de.lise.fluxflow.mongo.security.production.Boot4ProductionMongoCustomTypeKeyIT",
        "de.lise.fluxflow.mongo.security.production.Boot4ProductionMongoReconciliationIT",
    ))
}

kotlin.sourceSets.named("test") {
    kotlin.srcDir("../../security-tests/src/test/kotlin")
}
sourceSets.named("test") {
    java.srcDir("../../security-tests/src/test/java")
}

dependencyManagement {
    imports {
        mavenBom("org.testcontainers:testcontainers-bom:2.0.5")
    }
}

kotlin {
    sourceSets {
        main {
            kotlin.srcDir("../../springboot/springboot-mongo/src/main/kotlin")
            resources.srcDir("../../springboot/springboot-mongo/src/main/resources")
        }
    }
}

dependencies {
    implementation(kotlin("reflect"))

    implementation("org.springframework.data:spring-data-mongodb")
    implementation("org.springframework.boot:spring-boot-autoconfigure")

    implementation("org.mongodb:mongodb-driver-sync")

    implementation(project(":core:api"))
    implementation(project(":core:persistence"))
    implementation(project(":core:reflection"))
    implementation(project(":core:migration"))

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-mongodb")
    testImplementation("org.testcontainers:testcontainers")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-mongodb")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation(project(":springboot4:springboot"))
    testImplementation(project(":springboot4:springboot-testing"))
    testImplementation(project(":core:engine"))
    testImplementation(project(":core:scheduling"))
    testImplementation(project(":core:stereotyped"))
    testImplementation(project(":core:validation"))
}
