dependencies {
    api(project(":api"))
    api(project(":engine"))
    api(project(":stereotyped"))


    implementation(kotlin("reflect"))

    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot-autoconfigure")

    implementation(project(":reflection"))
    implementation(project(":persistence"))
    implementation(project(":scheduling"))
    implementation(project(":validation"))


    runtimeOnly(project(":springboot-test-scheduling"))


    testImplementation(project(":testing"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}