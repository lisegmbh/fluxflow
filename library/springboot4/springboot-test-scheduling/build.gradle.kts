kotlin {
    sourceSets {
        main {
            kotlin.srcDir("../../springboot/springboot-test-scheduling/src/main/kotlin")
            resources.srcDir("../../springboot/springboot-test-scheduling/src/main/resources")
        }
    }
}

dependencies {
    api(project(":core:scheduling"))
    implementation(project(":core:test-scheduling"))

    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
}
