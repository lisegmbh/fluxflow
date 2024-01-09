dependencies {
    api(project(":scheduling"))
    implementation(project(":test-scheduling"))

    implementation("org.springframework:spring-context:6.1.2")
    implementation("org.springframework.boot:spring-boot-autoconfigure:3.2.1")
}