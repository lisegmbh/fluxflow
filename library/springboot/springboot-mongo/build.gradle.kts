import de.lise.fluxflow.gradle.SecurityTestRequirements

plugins {
    id("fluxflow.security-tests")
}

kotlin.sourceSets.named("main") {
    kotlin.srcDir("src/boot3/kotlin")
}

extensions.configure<SecurityTestRequirements>("securityTests") {
    requiredClasses.set(listOf(
        "de.lise.fluxflow.mongo.security.baseline.SecurityWitnessTest",
        "de.lise.fluxflow.mongo.security.baseline.ActivationBaselineTest",
        "de.lise.fluxflow.mongo.security.production.Boot3ProductionMongoSecurityIT",
        "de.lise.fluxflow.mongo.security.production.Boot3ProductionMongoReconciliationIT",
    ))
}

kotlin.sourceSets.named("test") {
    kotlin.srcDir("../../security-tests/src/test/kotlin")
}
sourceSets.named("test") {
    java.srcDir("../../security-tests/src/test/java")
}

// Boot's dependency-management rules take precedence over a Gradle platform.
// Keep the Boot 3-compatible Testcontainers line, including its Docker API fix.
dependencyManagement {
    imports {
        mavenBom("org.testcontainers:testcontainers-bom:1.21.4")
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
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:mongodb")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation(project(":core:engine"))
    testImplementation(project(":core:scheduling"))
    testImplementation(project(":core:stereotyped"))
    testImplementation(project(":core:validation"))
    testImplementation(project(":springboot:springboot"))
}
