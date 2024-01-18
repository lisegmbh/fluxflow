dependencies {
    implementation(project(":reflection"))

    implementation("org.springframework:spring-web")
    implementation("org.springframework:spring-context")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.16.1")
    implementation("jakarta.servlet:jakarta.servlet-api:6.0.0")

    implementation(kotlin("reflect"))
}