dependencies {
    implementation(project(":core:reflection"))

    implementation("org.springframework:spring-web")
    implementation("org.springframework:spring-context")

    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("jakarta.servlet:jakarta.servlet-api")

    implementation(kotlin("reflect"))
}