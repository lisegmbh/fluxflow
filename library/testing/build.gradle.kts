dependencies {
    implementation("org.springframework:spring-context")
    implementation(project(":springboot"))

    api(project(":springboot-in-memory-persistence"))
    api(project(":test-persistence"))
    api(project(":test-scheduling"))
}