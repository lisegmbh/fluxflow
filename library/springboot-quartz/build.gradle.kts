dependencies {
    api(project(":scheduling"))
    api("org.springframework.boot:spring-boot-starter-quartz")
    implementation("org.liquibase:liquibase-core:4.24.0")
}