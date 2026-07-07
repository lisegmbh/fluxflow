kotlin {
    sourceSets {
        main {
            // Only the Boot-version-independent sources are shared with :springboot:springboot-web.
            // The Jackson-dependent classes have Jackson 3 counterparts in this module's own
            // src/main/kotlin (Spring Boot 4 auto-configures a Jackson 3 ObjectMapper).
            kotlin.srcDir("../../springboot/springboot-web/src/shared/kotlin")
        }
    }
}

dependencies {
    implementation(project(":core:reflection"))

    implementation("org.springframework:spring-web")
    implementation("org.springframework:spring-context")

    implementation("tools.jackson.core:jackson-databind")
    // Required at runtime so the Boot-4-auto-configured ObjectMapper can deserialize
    // request bodies into Kotlin (data) classes.
    runtimeOnly("tools.jackson.module:jackson-module-kotlin")
    implementation("jakarta.servlet:jakarta.servlet-api")

    implementation(kotlin("reflect"))

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-json")
}
