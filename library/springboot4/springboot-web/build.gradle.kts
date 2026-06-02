kotlin {
    sourceSets {
        main {
            kotlin.srcDir("../../springboot/springboot-web/src/main/kotlin")
            resources.srcDir("../../springboot/springboot-web/src/main/resources")
        }
    }
}

dependencies {
    implementation(project(":core:reflection"))

    implementation("org.springframework:spring-web")
    implementation("org.springframework:spring-context")

    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("jakarta.servlet:jakarta.servlet-api")

    implementation(kotlin("reflect"))

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-json")
}
