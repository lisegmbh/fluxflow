kotlin {
    sourceSets {
        main {
            kotlin.srcDir("../../springboot/springboot-in-memory-persistence/src/main/kotlin")
            resources.srcDir("../../springboot/springboot-in-memory-persistence/src/main/resources")
        }
    }
}

dependencies {
    api(project(":core:test-persistence"))
    implementation("org.springframework:spring-context")
}
