dependencies {
    implementation("org.springframework:spring-context:6.0.13")
    implementation("org.springframework.boot:spring-boot-autoconfigure:3.1.5")
    
    implementation(kotlin("reflect"))
    
    implementation(project(":api"))
    implementation(project(":stereotyped"))
    implementation(project(":reflection"))
    implementation(project(":engine"))
    implementation(project(":persistence"))
    implementation(project(":scheduling"))
    implementation(project(":validation"))

    testImplementation(project(":testing"))
    testImplementation("org.springframework.boot:spring-boot-starter-test:3.2.0")
}