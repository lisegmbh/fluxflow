dependencies {
    implementation("org.springframework:spring-context")
    implementation(project(":springboot4:springboot"))

    api(project(":springboot4:springboot-in-memory-persistence"))
    api(project(":core:test-persistence"))
    api(project(":core:test-scheduling"))
}
