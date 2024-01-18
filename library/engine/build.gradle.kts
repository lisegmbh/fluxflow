dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.9.21")
    
    implementation(project(":api"))
    implementation(project(":persistence"))
    implementation(project(":scheduling"))
    implementation(project(":reflection"))
    implementation(project(":stereotyped"))

    testImplementation("org.springframework.boot:spring-boot-starter-test:3.2.1")
    testImplementation(project(":testing"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
}