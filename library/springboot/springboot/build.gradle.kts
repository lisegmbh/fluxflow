dependencies {
    api(project(":core:api"))
    api(project(":core:engine"))
    api(project(":core:stereotyped"))


    implementation(kotlin("reflect"))

    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot-autoconfigure")

    implementation(project(":core:reflection"))
    implementation(project(":core:persistence"))
    implementation(project(":core:scheduling"))
    implementation(project(":core:validation"))
    implementation(project(":core:migration"))


    runtimeOnly(project(":springboot:springboot-test-scheduling"))


    testImplementation(project(":springboot:springboot-testing"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}