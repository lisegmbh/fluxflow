dependencies {
    implementation("org.springframework:spring-context")
    implementation(project(":springboot:springboot"))

    api(project(":springboot:springboot-in-memory-persistence"))
    api(project(":core:test-persistence"))
    api(project(":core:test-scheduling"))
}