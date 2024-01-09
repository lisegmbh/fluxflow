dependencies {
    implementation("org.springframework:spring-context:6.0.13")
    implementation("org.springframework.boot:spring-boot-autoconfigure:3.1.5")
    
    implementation(kotlin("reflect"))
    
    api(project(":api"))
    api(project(":engine"))
    runtimeOnly(project(":springboot-test-scheduling"))

    implementation(project(":stereotyped"))
    implementation(project(":reflection"))
    implementation(project(":persistence"))
    implementation(project(":scheduling"))
    implementation(project(":validation"))

    testImplementation(project(":testing"))
    testImplementation("org.springframework.boot:spring-boot-starter-test:3.2.0")
}