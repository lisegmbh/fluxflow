dependencies {
    api(project(":scheduling"))
    implementation(project(":test-scheduling"))

    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
}