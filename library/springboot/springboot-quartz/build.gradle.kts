dependencies {
    api(project(":core:scheduling"))
    api("org.springframework.boot:spring-boot-starter-quartz")
    implementation("org.liquibase:liquibase-core:4.33.0")
}