kotlin {
    sourceSets {
        main {
            // Boot-version-independent sources, also compiled into :springboot4:springboot-web.
            // Jackson-dependent sources stay in src/main/kotlin (Jackson 2); the spring4 module
            // provides its own Jackson 3 counterparts.
            kotlin.srcDir("src/shared/kotlin")
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

    testImplementation("org.springframework:spring-test")
}