dependencies {
    api(project(":core:scheduling"))
    api("org.springframework.boot:spring-boot-starter-quartz")
    implementation("org.liquibase:liquibase-core:5.0.3")
}