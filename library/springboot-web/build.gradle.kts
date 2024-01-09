dependencies {
    implementation(project(":reflection"))

    implementation("org.springframework:spring-web:6.1.1")
    implementation("org.springframework:spring-context:6.0.13")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.16.0")
    implementation("jakarta.servlet:jakarta.servlet-api:6.0.0")

    implementation(kotlin("reflect"))
}