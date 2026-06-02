kotlin {
    sourceSets {
        main {
            kotlin.srcDir("../../springboot/springboot-quartz/src/main/kotlin")
            resources.srcDir("../../springboot/springboot-quartz/src/main/resources")
        }
    }
}

dependencies {
    api(project(":core:scheduling"))
    api("org.springframework.boot:spring-boot-starter-quartz")
    implementation("org.liquibase:liquibase-core")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(project(":springboot4:springboot"))
}
