dependencies {
    api(project(":core:scheduling"))
    implementation(project(":core:test-scheduling"))

    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
}