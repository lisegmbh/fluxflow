dependencies {
    implementation(project(":core:reflection"))

    implementation("org.springframework:spring-web")
    implementation("org.springframework:spring-context")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.2")
    implementation("jakarta.servlet:jakarta.servlet-api:6.1.0")

    implementation(kotlin("reflect"))
}