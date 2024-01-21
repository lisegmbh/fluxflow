dependencies {
    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot-autoconfigure")

    implementation(project(":api"))
    implementation(project(":engine"))
    implementation(project(":persistence"))
}