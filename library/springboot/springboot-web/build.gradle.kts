dependencies {
    implementation(project(":core:reflection"))

    implementation("org.springframework:spring-web")
    implementation("org.springframework:spring-context")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.0")
    implementation("jakarta.servlet:jakarta.servlet-api:6.0.0")

    implementation(kotlin("reflect"))
}