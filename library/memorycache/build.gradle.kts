dependencies {
    implementation("org.springframework:spring-context:6.0.13")
    implementation("org.springframework.boot:spring-boot-autoconfigure:3.1.5")

    implementation(project(":api"))
    implementation(project(":engine"))
    implementation(project(":persistence"))
}