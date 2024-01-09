dependencies {
    api(project(":scheduling"))
    api("org.springframework.boot:spring-boot-starter-quartz:3.1.5")
    implementation("org.liquibase:liquibase-core:4.24.0")
}