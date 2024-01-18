dependencies {
    api(project(":api"))
    api(project(":engine"))
    api(project(":stereotyped"))


    implementation(kotlin("reflect"))

    implementation("org.springframework:spring-context:6.0.13")
    implementation("org.springframework.boot:spring-boot-autoconfigure:3.1.5")

    implementation(project(":reflection"))
    implementation(project(":persistence"))
    implementation(project(":scheduling"))
    implementation(project(":validation"))


    runtimeOnly(project(":springboot-test-scheduling"))


    testImplementation(project(":testing"))
    testImplementation("org.springframework.boot:spring-boot-starter-test:3.2.1")
}